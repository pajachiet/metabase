(ns metabase.query-processor.middleware.add-references.references)

(defn query-references [{[merged-select] :qp.references/keys, :as inner-query}]
  (or (when merged-select
        ))
  (or merged-select
      ))
