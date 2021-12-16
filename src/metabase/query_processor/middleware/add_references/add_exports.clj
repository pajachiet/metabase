(ns metabase.query-processor.middleware.add-references.add-exports
  (:require
   [metabase.mbql.util :as mbql.u]
   [metabase.query-processor.middleware.add-references.add-merged-select
    :as
    add-merged-select]))

(defn- export-info [{:keys [clause source alias], :as info}]
  (mbql.u/match-one clause
    [:field id-or-name (m :guard :join-alias)]
    (export-info (assoc info
                        :table (:join-alias m)
                        :clause (mbql.u/update-field-options &match dissoc :join-alias) ))
    _
    info))

(defn- exports [merged-select]
  (into [] (map export-info) merged-select))

(defn- add-exports** [{:qp.references/keys [merged-select], :as inner-query}]
  (assoc inner-query :qp.references/exports (exports merged-select)))

(defn- add-exports* [inner-query]
  (mbql.u/replace inner-query
    (m :guard (every-pred :qp.references/merged-select (complement :qp.references/exports)))
    (add-exports* (add-exports** m))))

(defn add-exports [{query-type :type, :as query}]
  (let [query (add-merged-select/add-merged-select query)]
    (if-not (= query-type :query)
      query
      (update query :query add-exports*))))


;; NOCOMMIT
(defn x []
  (metabase.test/with-everything-store
    (add-exports
     (metabase.test/mbql-query venues
       {:fields       [$id $name $category_id->&CATEGORIES__via__CATEGORY_ID.categories.name]
        :joins        [{:strategy     :left-join
                        :alias        "CATEGORIES__via__CATEGORY_ID"
                        :condition    [:= $category_id &CATEGORIES__via__CATEGORY_ID.categories.id]
                        :source-table $$categories
                        :fk-field-id  182}]
        :source-query {:source-table $$venues
                       :fields       [$id $name $category_id $latitude $longitude $price]}}))))
