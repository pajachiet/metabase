(ns metabase.query-processor.middleware.add-references.add-merged-select
  (:require
   [metabase.driver.util :as driver.u]
   [metabase.mbql.schema :as mbql.s]
   [metabase.mbql.schema.helpers :as mbql.s.helpers]
   [metabase.mbql.util :as mbql.u]
   [metabase.query-processor.error-type :as qp.error-type]
   [metabase.query-processor.middleware.add-references.alias :as alias]
   [metabase.util.i18n :refer [tru]]
   [schema.core :as s]))

(defn add-unambiguous-alias [driver {:keys [clause source], :as m}]
  (assoc m :alias (alias/clause-alias driver clause)))

(defn- deduplicate-aliases [rf]
  (let [unique-name-fn (mbql.u/unique-name-generator)]
    ((map (fn [info]
            (update info :alias unique-name-fn))) rf)))

(def SelectInfo
  {:clause   mbql.s/FieldOrAggregationReference
   :alias    mbql.s.helpers/NonBlankString
   ;; :table-alias su/NonBlankString
   s/Keyword s/Any
   ;; TODO -- :source ?
   })

(def SelectInfos
  (-> [SelectInfo] mbql.s.helpers/distinct #_mbql.s.helpers/non-empty))

(s/defn merged-select :- SelectInfos
  [driver inner-query :- mbql.s/MBQLQuery]
  (into
   []
   (comp (map (fn [k]
                (map-indexed
                 (fn [i clause]
                   {:clause clause
                    :source k
                    ::index i})
                 (get inner-query k))))
         cat
         (map (partial add-unambiguous-alias driver))
         deduplicate-aliases
         (map (fn [{:keys [source], ::keys [index], :as m}]
                (cond-> m (= source :aggregation) (assoc :clause [:aggregation index]))))
         (map #(dissoc % ::index)))
   [:breakout :aggregation :fields]))

(defn- add-merged-select-one-level
  [driver {:keys [source-query joins], :as inner-query}]
  (try
    (let [add-merged-select-one-level* (partial add-merged-select-one-level driver)
          inner-query                  (cond-> inner-query
                                         source-query (update :source-query add-merged-select-one-level*)
                                         (seq joins)  (update :joins (partial mapv add-merged-select-one-level*)))
          select                       (merged-select driver inner-query)]
      (cond-> inner-query
        (seq select) (assoc :qp.references/merged-select select)))
    (catch Throwable e
      (throw (ex-info (tru "Error adding merged select: {0}" (ex-message e))
                      {:type qp.error-type/qp, :driver driver, :query inner-query}
                      e)))))

(defn add-merged-select
  ([{:keys [database], :as query}]
   (add-merged-select (driver.u/database->driver database) query))

  ([driver {query-type :type, :as query}]
   (cond-> query
     (= query-type :query) (update :query (partial add-merged-select-one-level driver)))))
