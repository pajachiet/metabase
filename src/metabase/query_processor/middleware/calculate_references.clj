(ns metabase.query-processor.middleware.calculate-references
  "Middleware that additional columns to a query with information about the columns that get returned at each point in a
  query and ones that are visible to that level of the query. Information includes canonical field refs as well as
  unambiguous aliases.")
