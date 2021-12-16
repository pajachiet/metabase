(ns metabase.query-processor.middleware.add-references.add-merged-select-test
  (:require
   [clojure.test :refer :all]
   [metabase.query-processor.middleware.add-references.add-merged-select
    :as add-merged-select]
   [metabase.test :as mt]))

(deftest add-merged-select-test
  (mt/with-everything-store
    (testing "basic query with `:breakout` and `:order-by`"
      (is (query= (mt/mbql-query venues
                    {:breakout                    [$price]
                     :aggregation                 [[:aggregation-options [:count] {:name "count"}]]
                     :order-by                    [[:asc $price]]
                     :qp.references/merged-select [{:clause $price, :source :breakout, :alias "PRICE"}
                                                   {:clause [:aggregation 0], :source :aggregation, :alias "count"}]})
                  (add-merged-select/add-merged-select
                   (mt/mbql-query venues
                     {:breakout    [$price]
                      :aggregation [[:aggregation-options [:count] {:name "count"}]]
                      :order-by    [[:asc $price]]})))))

    (testing "Join that causes Fields to get included"
      (is (query= (mt/mbql-query venues
                    {:fields                      [$id $name $category_id $latitude $longitude $price &c.categories.id &c.categories.name]
                     :joins                       [{:strategy     :left-join
                                                    :source-table $$categories
                                                    :alias        "c"
                                                    :condition    [:= $category_id &c.categories.id]}]
                     :qp.references/merged-select [{:clause $id, :source :fields, :alias "ID"}
                                                   {:clause $name, :source :fields, :alias "NAME"}
                                                   {:clause $category_id, :source :fields, :alias "CATEGORY_ID"}
                                                   {:clause $latitude, :source :fields, :alias "LATITUDE"}
                                                   {:clause $longitude, :source :fields, :alias "LONGITUDE"}
                                                   {:clause $price, :source :fields, :alias "PRICE"}
                                                   {:clause &c.categories.id, :source :fields, :alias "c__ID"}
                                                   {:clause &c.categories.name, :source :fields, :alias "c__NAME"}]})
                  (add-merged-select/add-merged-select
                   (mt/mbql-query venues
                     {:fields [$id $name $category_id $latitude $longitude $price &c.categories.id &c.categories.name]
                      :joins  [{:strategy     :left-join
                                :source-table $$categories
                                :alias        "c"
                                :condition    [:= $category_id &c.categories.id]}]})))))

    (testing "Query with source query"
      (is (query= (mt/mbql-query categories
                    {:source-query                {:source-table                $$categories
                                                   :fields                      [$id $name]
                                                   :qp.references/merged-select [{:clause $id
                                                                                  :source :fields
                                                                                  :alias  "ID"}
                                                                                 {:clause $name
                                                                                  :source :fields
                                                                                  :alias  "NAME"}]}
                     :fields                      [$id $name]
                     :qp.references/merged-select [{:clause $id
                                                    :source :fields
                                                    :alias  "ID"}
                                                   {:clause $name
                                                    :source :fields
                                                    :alias  "NAME"}]})
                  (add-merged-select/add-merged-select
                   (mt/mbql-query categories
                     {:source-query {:source-table $$categories
                                     :fields       [$id $name]}
                      :fields       [$id $name]})))))

    (testing "Query with duplicate column names"
      (is (query= (mt/mbql-query venues
                    {:fields                      [$id $name $category_id->&CATEGORIES__via__CATEGORY_ID.categories.name]
                     :joins                       [{:strategy     :left-join
                                                    :alias        "CATEGORIES__via__CATEGORY_ID"
                                                    :condition    [:= $category_id &CATEGORIES__via__CATEGORY_ID.categories.id]
                                                    :source-table $$categories
                                                    :fk-field-id  182}]
                     :source-query                {:source-table                $$venues
                                                   :fields                      [$id $name $category_id $latitude $longitude $price]
                                                   :qp.references/merged-select [{:clause $id, :source :fields, :alias "ID"}
                                                                                 {:clause $name, :source :fields, :alias "NAME"}
                                                                                 {:clause $category_id, :source :fields, :alias "CATEGORY_ID"}
                                                                                 {:clause $latitude, :source :fields, :alias "LATITUDE"}
                                                                                 {:clause $longitude, :source :fields, :alias "LONGITUDE"}
                                                                                 {:clause $price, :source :fields, :alias "PRICE"}]}
                     :qp.references/merged-select [{:clause $id, :source :fields, :alias "ID"}
                                                   {:clause $name, :source :fields, :alias "NAME"}
                                                   {:clause $category_id->&CATEGORIES__via__CATEGORY_ID.categories.name
                                                    :source :fields
                                                    :alias  "CATEGORIES__via__CATEGORY_ID__NAME"}]})
                  (add-merged-select/add-merged-select
                   (mt/mbql-query venues
                     {:fields       [$id $name $category_id->&CATEGORIES__via__CATEGORY_ID.categories.name]
                      :joins        [{:strategy     :left-join
                                      :alias        "CATEGORIES__via__CATEGORY_ID"
                                      :condition    [:= $category_id &CATEGORIES__via__CATEGORY_ID.categories.id]
                                      :source-table $$categories
                                      :fk-field-id  182}]
                      :source-query {:source-table $$venues
                                     :fields       [$id $name $category_id $latitude $longitude $price]}})))))

    (testing "expressions"
      (is (query= (mt/mbql-query venues
                    {:expressions                 {:wow [:- [:* $price 2] [:+ $price 0]]}
                     :limit                       3
                     :order-by                    [[:asc $id]]
                     :fields                      [$id $name $category_id $latitude $longitude $price [:expression "wow"]]
                     :qp.references/merged-select [{:clause $id, :source :fields, :alias "ID"}
                                                   {:clause $name, :source :fields, :alias "NAME"}
                                                   {:clause $category_id, :source :fields, :alias "CATEGORY_ID"}
                                                   {:clause $latitude, :source :fields, :alias "LATITUDE"}
                                                   {:clause $longitude, :source :fields, :alias "LONGITUDE"}
                                                   {:clause $price, :source :fields, :alias "PRICE"}
                                                   {:clause [:expression "wow"], :source :fields, :alias "wow"}]})
                  (add-merged-select/add-merged-select
                   (mt/mbql-query venues
                     {:expressions {:wow [:- [:* $price 2] [:+ $price 0]]}
                      :limit       3
                      :order-by    [[:asc $id]]
                      :fields      [$id $name $category_id $latitude $longitude $price [:expression "wow"]]})))))))
