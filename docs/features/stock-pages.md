# Stock pages

Grocy's stock entries, stock journal and journal summary (`stock/`, `journal/` of
`feature:stock`); they open the product overview (`product-overview.md`).

- Routes `StockEntriesRoute`, `StockJournalRoute`, `StockJournalSummaryRoute` of
  `feature:stock:api`, opened from the product form's toolbar and the product overview's
  buttons. `StockRepository` (`core:data`) stores the entries (`stock_entries`, with the
  amounts) and the journal (`stock_log`, a download of a date range replaces the bookings of that range) for
  offline use; bookings (consume, consume as spoiled, open, edit, undo) are online only and
  download the entries again. Entries show Grocy's columns and due highlighting
  (`dueStatusOf`: overdue/expired by due type, due soon by `stock_due_soon_days`), price per
  price unit; their menu holds Grocy's other actions in its groups. Booking messages offer
  "Undo" in the snackbar. The journal filters by product, type, location, user and Grocy's
  date ranges (`journalSince`, SQLite month arithmetic), searches, undoes; the summary sums
  every booking not undone per product, type and user (Grocy's view is not exposed).
