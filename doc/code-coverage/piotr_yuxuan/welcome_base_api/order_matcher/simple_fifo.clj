✔ (ns piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo
?   (:require [malli.core :as m]
?             [xtdb.api :as xt])
?   (:import (java.util UUID)))
  
? ;;; https://en.wikipedia.org/wiki/Order_(exchange)
? ;
? ;(def order-types
? ;  "See [MIT201](https://docs.londonstockexchange.com/sites/default/files/documents/MIT201%20-%20Guide%20to%20the%20Trading%20System%20Issue%2015.2.pdf) page 39."
? ;  {:limit "A Limit Order is an anonymous priced order that is fully displayed when persistent in an order book and may execute at prices equal to or better than its limit price. Limit Orders never have price priority over market orders."
? ;   :market "A Market Order is un-priced, and therefore not price forming, but has price priority over all priced orders. Market Orders cannot persist on the order book during the regular scheduled trading session but will during an auction if they have an appropriate Time in Force (this includes where the incoming Market Order actually triggers an AESP auction call). Any that remain unexecuted following the completion of the auction will be automatically deleted."
? ;   :stop-limit "A Stop Limit Order is a Limit Order that will remain unelected (will not be entered into order book) until the stop price is reached. Once elected, a Stop Limit Order will be treated as a regular Limit Order."
? ;   :stop "A Stop Order is a Market Order that will remain unelected (will not be entered into order book) until the stop price is reached. Once elected, it will be treated similar to a regular Market Order."
? ;   :iceberg "An Iceberg Order publicly displays only a portion of its total volume that is available for execution. The maximum displayed amount, known as the peak size, and the total size of the order can be specified by the participant and must be above specified minimums. Where enabled, customers have the option to have the refreshed peak size randomised. On each peak refresh, the size will be randomised within a set band above the value of the initial peak size entered with parameters published in the Business Parameters Document."
? ;   :passive "On entry order will only immediately execute against non-visible orders that are better than touch, any remaining quantity will then only be added to the order book if it is within the number of visible price points from the prevailing BBO prescribed by the submitter."
? ;   :hidden-limit "Non-displayed limit order that on entry must exceed in size the relevant MIN RESERVE ORDER VALUE trading parameter. It is not possible to apply a Minimum Execution Size on a Hidden Limit Order."
? ;   :mid-price-pegged ""})
? ;
? ;(def Order
? ;  "See [MIT201](https://docs.londonstockexchange.com/sites/default/files/documents/MIT201%20-%20Guide%20to%20the%20Trading%20System%20Issue%2015.2.pdf) page 42."
? ;  (m/schema
? ;    [:map {:closed true}
? ;     [:instrument [string? {:description "The unique identifier of the security"}]]
? ;     [:side [:enum {:description "Whether the order is to buy or sell"}
? ;             :buy :sell]]
? ;     [:type [:enum {:description "The type of the order, in conjunction with Order Sub Type (Native) or DisplayMethod (FIX)"}
? ;             :market
? ;             :limit
? ;             :stop
? ;             :stop-limit
? ;             :pegged
? ;             :random-peak
? ;             :size-iceberg
? ;             :offset]]
? ;     [:time-in-force]]))
? ;
? ;;; Sale = Ask (who wants it?)
? ;;; Purchase = Bid (I propose this price!)\
? ;
? ;;; Here, all orders are IOC. Immediate or Cancel: executed on entry, with any remaining unexecuted volume expired.
? ;
? ;(defonce current-order
? ;  {:xt/id (UUID/randomUUID)
? ;   :client-id "A"
? ;   :side :ask
? ;   :quantity 5
? ;   :price 100})
? ;
? ;(defonce current-book
? ;  [{:xt/id (UUID/randomUUID)
? ;    :client-id "B"
? ;    :side :bid
? ;    :quantity 1
? ;    :price 99}
? ;   {:xt/id (UUID/randomUUID)
? ;    :client-id "C"
? ;    :side :bid
? ;    :quantity 2
? ;    :price 100}
? ;   {:xt/id (UUID/randomUUID)
? ;    :client-id "D"
? ;    :side :bid
? ;    :quantity 2
? ;    :price 100}
? ;   {:xt/id (UUID/randomUUID)
? ;    :client-id "E"
? ;    :side :bid
? ;    :quantity 1
? ;    :price 101}])
? ;
? ;(def opposite-side
? ;  {:ask :bid
? ;   :bid :ask})
? ;
? ;(defonce node
? ;  (xt/start-node {}))
? ;
? ;(comment
? ;  (->> current-book
? ;       (cons current-order)
? ;       (map #(do [::xt/put %]))
? ;       (xt/submit-tx node)))
? ;
? ;(time (xt/q
? ;        (xt/db node)
? ;        '{:find [id price]
? ;          :keys [id price]
? ;          :in [current-price opposite-side]
? ;          :where [[order :xt/id id]
? ;                  [order :side opposite-side]
? ;                  [order :client-id client-id]
? ;                  [order :price price]
? ;                  [(<= current-price price)]]
? ;          :order-by [[price :asc]]}
? ;        (:price current-order)
? ;        (get opposite-side (:side current-order))))
? ;
? ;(defn process-order
? ;  [current-book current-order]
? ;  (let [interesting-side (get opposite-side (:side current-order))]
? ;    (reduce (fn [acc interested-order]
? ;              (let [available-quantity (min (:quantity interested-order) (:quantity current-order))]
? ;                (println :available-quantity available-quantity))
? ;              acc)
? ;            {:book current-book
? ;             :trade []
? ;             :rejected-orders []}
? ;            (->> current-book
? ;                 (filter (comp #{interesting-side} :side))
? ;                 (filter (comp #(<= % (:price current-order)) :price))))))
? ;
? ;#_(defn match-orders
? ;    "Take a book of standing orders, execute order in a first-in, first-out basis, and return trades and cancelled orders."
? ;    [book]
? ;    (let [[entry-order & standing-orders] book
? ;          opposite-side (-> entry-order :side opposite-side)
? ;          acceptable-prices? (if (= :bid (:side entry-order)) <= >=)]
? ;      (->> standing-orders
? ;           (filter (comp #{opposite-side} :side))
? ;           (filter (fn [standing-order]
? ;                     (acceptable-prices? (:price entry-order)
? ;                                         (:price standing-order))))
? ;           (reduce (fn [{:keys [remaining-order] :as acc} standing-order]
? ;                     (cond (not (pos? (:quantity remaining-order)))
? ;
? ;                           (<= (:quantity standing-order) (:quantity remaining-order))
? ;                           nil
? ;
? ;
? ;                           (<=1 (:quantity remaining-order))
? ;                           (let [quantity-bought (:quantity standing-order)]
? ;                             (-> acc
? ;                                 (update-in [remaining-order :quantity])))))
? ;                   {:remaining-order entry-order
? ;                    :trades []
? ;                    :executed-orders []
? ;                    :cancelled-orders []}))))
