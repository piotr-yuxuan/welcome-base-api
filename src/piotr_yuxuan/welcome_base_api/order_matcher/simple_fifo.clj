(ns piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo
  "This should be so simple it's can't be wrong, but nonetheless well
  tested. Then it may serve as a reference implementation for later
  more optimised strategies."
  (:require [piotr-yuxuan.welcome-base-api.tick :as tick]
            [clojure.data.avl :as avl]
            [malli.core :as m])
  (:import (java.util UUID)))

;; https://en.wikipedia.org/wiki/Order_(exchange)

(def order-types
  "See [MIT201](https://docs.londonstockexchange.com/sites/default/files/documents/MIT201%20-%20Guide%20to%20the%20Trading%20System%20Issue%2015.2.pdf) page 39."
  {:limit "A Limit Order is an anonymous priced order that is fully displayed when persistent in an order book and may execute at prices equal to or better than its limit price. Limit Orders never have price priority over market orders."
   :market "A Market Order is un-priced, and therefore not price forming, but has price priority over all priced orders. Market Orders cannot persist on the order book during the regular scheduled trading session but will during an auction if they have an appropriate Time in Force (this includes where the incoming Market Order actually triggers an AESP auction call). Any that remain unexecuted following the completion of the auction will be automatically deleted."
   :stop-limit "A Stop Limit Order is a Limit Order that will remain unelected (will not be entered into order book) until the stop price is reached. Once elected, a Stop Limit Order will be treated as a regular Limit Order."
   :stop "A Stop Order is a Market Order that will remain unelected (will not be entered into order book) until the stop price is reached. Once elected, it will be treated similar to a regular Market Order."
   :iceberg "An Iceberg Order publicly displays only a portion of its total volume that is available for execution. The maximum displayed amount, known as the peak size, and the total size of the order can be specified by the participant and must be above specified minimums. Where enabled, customers have the option to have the refreshed peak size randomised. On each peak refresh, the size will be randomised within a set band above the value of the initial peak size entered with parameters published in the Business Parameters Document."
   :passive "On entry order will only immediately execute against non-visible orders that are better than touch, any remaining quantity will then only be added to the order book if it is within the number of visible price points from the prevailing BBO prescribed by the submitter."
   :hidden-limit "Non-displayed limit order that on entry must exceed in size the relevant MIN RESERVE ORDER VALUE trading parameter. It is not possible to apply a Minimum Execution Size on a Hidden Limit Order."
   :mid-price-pegged ""})

(def Order
  "See [MIT201](https://docs.londonstockexchange.com/sites/default/files/documents/MIT201%20-%20Guide%20to%20the%20Trading%20System%20Issue%2015.2.pdf) page 42."
  (m/schema
    [:map {:closed true}
     [:instrument [string? {:description "The unique identifier of the security"}]]
     [:side [:enum {:description "Whether the order is to buy or sell"}
             :buy :sell]]
     [:type [:enum {:description "The type of the order, in conjunction with Order Sub Type (Native) or DisplayMethod (FIX)"}
             :market
             :limit
             :stop
             :stop-limit
             :pegged
             :random-peak
             :size-iceberg
             :offset]]
     [:time-in-force]]))

;; Sale = Ask (who wants it?)
;; Purchase = Bid (I propose this price!)\

;; Here, all orders are IOC. Immediate or Cancel: executed on entry, with any remaining unexecuted volume expired.

(defonce current-order
  {:xt/id (UUID/randomUUID)
   :client-id "A"
   :side :ask
   :quantity 5
   :price 100})

(defonce current-book
  [{:xt/id (UUID/randomUUID)
    :client-id "B"
    :side :bid
    :quantity 1
    :price 99}
   {:xt/id (UUID/randomUUID)
    :client-id "C"
    :side :bid
    :quantity 2
    :price 100}
   {:xt/id (UUID/randomUUID)
    :client-id "D"
    :side :bid
    :quantity 2
    :price 100}
   {:xt/id (UUID/randomUUID)
    :client-id "E"
    :side :bid
    :quantity 1
    :price 101}])

;; There is a difference between ask/bid and buy/sell. For example,
;; from the point of view of a seller, his order is an asking
;; `ask-order`, and the replies/bids he gets are `bid-orders`.

(defn split->matching+unmatching
  "Take a sequence of `bid-orders` alongside an `ask-order`. Two
  subsequences of updated matching/unmatching `bid-orders` are
  returned alongside the updated `ask-order`. Return `nil` instead of
  empty sequences.

  Return:
  ``` clojure
  [
   matched-ask-order ; If completely matched, `ask-order`.
   unmatched-ask-order ; If completely matched `nil`, otherwise a partial quantity.
   matching-bid-orders ; If not matched at all `nil`.
   unmatching-bid-orders ; If not matched at all same as input `bid-orders`.
  ]
  ```"
  [ask-order bid-orders]
  (let [bid-orders [{:side :bid
                     :quantity 1
                     :tick-value 0}
                    {:side :bid
                     :quantity 2
                     :tick-value 1}
                    {:side :bid
                     :quantity 1
                     :tick-value 0}
                    {:side :bid
                     :quantity 2
                     :tick-value -1}]
        ask-order {:side :ask
                   :quantity 2
                   :tick-value 0}]
    [ask-order nil bid-orders]
    {:ask-order-unmatched ask-order
     :bid-orders-matching nil
     :bid-orders-unmatched bid-orders}))

(defn remove-order-form-book
  [book order]
  (remove #{order} book))

(defn cancel-immediate?
  [order]
  (-> order
      :type
      #{:immediate-or-cancel :fill-or-kill}))

(defn order+matching-order->trade
  [now order matching-order]
  {:debitor-account (:account matching-order) ; Take money from this account and send it to the credit account.
   :creditor-account (:account order) ; Add Take money from the debit account and send it to the credit account
   :execution-time now ; Actual time this trade has been agreed, or executed.
   :settlement-time now ; Actual time the money transfer will materialize. Could also be called `delivery-time`.
   :tick-size nil
   :tick-value nil})

#_(defn dev-attempt
  "Take a symbol-market, an order, and return a maybe updated book and a maybe a trade."
  [{:keys [now bid-orders] :as security-market} ask-order]
  (let [bod-price 100M
        tick-radius (tick/radius :percent)
        tick-size (tick/size (tick/math-context tick-radius ^BigDecimal bod-price)
                             tick-radius
                             bod-price)
        ask-order {:side :ask
                   :quantity 2
                   :tick-value 0}
        bid-orders [{:side :bid
                     :quantity 2
                     :tick-value 0}
                    {:side :bid
                     :quantity 2
                     :tick-value 1}
                    {:side :bid
                     :quantity 2
                     :tick-value -1}]
        matching-order (match-order ask-order bid-orders)]
    (cond-> security-market
      (or matching-order (cancel-immediate? bid-orders)) (update :bid-orders remove-order-form-book matching-order)
      matching-order (update :trades conj (order+matching-order->trade now ask-order matching-order)))))
