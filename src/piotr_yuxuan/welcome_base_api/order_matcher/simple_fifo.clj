(ns piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo
  "This should be so simple it's can't be wrong, but nonetheless well
  tested. Then it may serve as a reference implementation for later
  more optimised strategies."
  (:require [malli.core :as m]
            [jmh.core :as jmh]
            [criterium.core :as c])
  (:import (java.util UUID)
           (java.math RoundingMode MathContext)))

(set! *unchecked-math* false)

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

(def opposite-side
  {:ask :bid
   :bid :ask})

(defn match-order
  "From an order, maybe return an order matching best from the book
  opposite, `nil` otherwise."
  [ask-order bid-order-book]
  (rand-nth bid-order-book))

(defn remove-order-form-book
  [book order]
  (remove #{order} book))

(defn cancel-immediate?
  [order]
  (-> order
      :type
      #{:immediate-or-cancel :fill-or-kill}))

;; Reading data from this experiment, I do not understand why performance on native shorts are so high.

(def a (rand-int (int (Math/sqrt Short/MAX_VALUE))))
(def b (rand-int (int (Math/sqrt Short/MAX_VALUE))))
(def a-BigDecimal (BigDecimal/valueOf (long a)))
(def b-BigDecimal (BigDecimal/valueOf (long b)))
(def a-long (long a))
(def b-long (long b))
(def a-Long (Long/valueOf (long a)))
(def b-Long (Long/valueOf (long b)))
(def a-integer (int a))
(def b-integer (int b))
(def a-Integer (Integer/valueOf (int a)))
(def b-Integer (Integer/valueOf (int b)))
(def a-short (short a))
(def b-short (short b))
(def a-Short (Short/valueOf (short a)))
(def b-Short (Short/valueOf (short b)))

(comment
  (doseq [[number-type a b] [[:BigDecimal a-BigDecimal b-BigDecimal]
                             [:long a-long b-long]
                             [:Long a-Long b-Long]
                             [:integer a-integer b-integer]
                             [:Integer a-Integer b-Integer]
                             [:short a-short b-short]
                             [:Short a-Short b-Short]]
          op-type [:checked :unchecked]]
    (println "+" number-type op-type)
    ;; => + :BigDecimal :checked, Execution time mean:   52.636327 ns
    ;; => + :BigDecimal :unchecked, Execution time mean: 51.552338 ns
    ;; => + :long :checked, Execution time mean:         15.986174 ns
    ;; => + :long :unchecked, Execution time mean:       13.179255 ns
    ;; => + :Long :checked, Execution time mean:         15.896992 ns
    ;; => + :Long :unchecked, Execution time mean:       13.585209 ns
    ;; => + :integer :checked, Execution time mean:      20.861606 ns
    ;; => + :integer :unchecked, Execution time mean:     4.681712 ns <- awesome, ten time faster than BigDecimal and three times faster than native short.
    ;; => + :Integer :checked, Execution time mean:      20.715196 ns
    ;; => + :Integer :unchecked, Execution time mean:    13.879221 ns
    ;; => + :short :checked, Execution time mean:        22.920586 ns
    ;; => + :short :unchecked, Execution time mean:      16.065464 ns
    ;; => + :Short :checked, Execution time mean:        24.081930 ns
    ;; => + :Short :unchecked, Execution time mean:      16.849061 ns

    ;; Variance may be moderately inflated by outliers but criterium isn't precise enough so as to care. Use JMH for statistically sound numbers.
    (cond (= :checked op-type)
          (binding [*unchecked-math* false]
            (+ a b) (c/bench (+ a b)))

          (and (= number-type :integer)
               (= :unchecked op-type))
          (binding [*unchecked-math* :warn-on-boxed]
            (unchecked-add-int a b) (c/bench (unchecked-add-int a b)))

          (= :unchecked op-type)
          (binding [*unchecked-math* :warn-on-boxed]
            (unchecked-add a b) (c/bench (unchecked-add a b))))))

;; At the beginning of the day, a tick value is at 0, meaning that the
;; price is at bod-price.  A tick value of `(int Short/MIN_VALUE)`
;; means that market value has been reduced to 0. A tick value of
;; (int Short/MAX_VALUE) means that market value has been multiplied
;; by 2. I chose to use the numeric type `integer` because it is the
;; fastest according to some unprecise tests benchmark below.

;; The value domain and type of market-value is BigDecimal, the value
;; domain of tick-value is that of Short while its type is Integer. A
;; tick-value is an approximation of a market-value which allow fast
;; and approximation-free computations.

;; As Y-fast trie datastructures are sensitive to the size of the
;; domain values, `short` then would be way better than `integer` but
;; the latter is way faster. A solution would be to use integer type
;; but restrict the value domain to that of shorts.  With things that
;; fast, no need to look into `SkipTrie` (= concurrent Y-fast trie)
;; yet.

(def short-tick-value-radius
  Short/MAX_VALUE)

(def market-type->tick-value-radius
  "As ticks are statically expressed with integers, we can't go more
  precise that `Integer/MAX_VALUE`. The precision increase the cost of
  lookups in Y-fast tries."
  {:percent 100
   :ten-thousandth 1e4
   :short short-tick-value-radius
   :integer Integer/MAX_VALUE})

(defn tick-value-radius
  [market-type]
  "Centered in 0. Set it to `[-100, 100]` for a percent precision. It
  might not be necessary be as large value as Short span."
  (int (get market-type->tick-value-radius market-type short-tick-value-radius)))

(defn tick-value-center
  [_market-type]
  0)

(defn tick-order-of-magnitude
  [market-type]
  "The smallest order of magnitude that can be represented in tick
  domain. Use one more (`inc`) to make sure you lose no information.

  ``` clojure
  (binding [*unchecked-math* :warn-on-boxed]
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      ;; some operations
      ))
  ```"
  (->> (tick-value-radius market-type)
       Math/log10
       Math/floor
       Math/round
       int))

(tick-order-of-magnitude :integer)

(defn tick-precision
  [market-type]
  "Tick precision is a ratio. Tick size is relative to the market value at the beginning of day."
  (binding [*unchecked-math* :warn-on-boxed]
    ;; Isn't it a issue to create an implicit MathContext every time?
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      (->> (tick-value-radius market-type)
           (/ 1)))))

(def bod-ticks
  "By definition. tick-value can vary within `(tick-value-span market-type)`."
  (int 0))

(defn ^BigDecimal tick-size
  [market-type ^BigDecimal bod-price]
  (binding [*unchecked-math* :warn-on-boxed]
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      (->> ^int (tick-value-radius market-type)
           BigDecimal.
           (/ bod-price)))))

(defn bod-price+market-value->tick-value
  [market-type ^BigDecimal bod-price ^BigDecimal market-value]
  (binding [*unchecked-math* :warn-on-boxed]
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      (-> (- market-value bod-price)
          ^BigDecimal (/ (tick-size market-type bod-price))
          (.setScale 0 RoundingMode/HALF_EVEN)
          (.intValue)))))

(defn ^BigDecimal bod-price+tick-value->market-value
  [market-type ^BigDecimal bod-price tick-value]
  (binding [*unchecked-math* :warn-on-boxed]
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      (-> (tick-size market-type bod-price)
          (* tick-value)
          (+ bod-price)))))

(defn ^BigDecimal tick-size+tick-value->market-value
  [market-type tick-size tick-value]
  (binding [*unchecked-math* :warn-on-boxed]
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      (BigDecimal. ^int (* tick-size tick-value)))))

tick-size+tick-value->market-value

(defn order+matching-order->trade
  [now order matching-order]
  {:debitor-account (:account matching-order) ; Take money from this account and send it to the credit account.
   :creditor-account (:account order) ; Add Take money from the debit account and send it to the credit account
   :execution-time now ; Actual time this trade has been agreed, or executed.
   :settlement-time now ; Actual time the money transfer will materialize. Could also be called `delivery-time`.
   :tick-size nil
   :tick-value nil})

;; A tick is just a number, it is not money. We take the price at the
;; beginning of day (bod-price) and span a range of integers from -1e3
;; to 1e3 that represents all the possible price values around ±10% of
;; bod-price. This means that the actual price of a deal is completely
;; defined by the bod-price of the market and a tick.

;; FIXME Context is the new substrate, this no longer means anything.
(defn ask-bid-rename-me-1
  "Take a symbol-market, an order, and return a maybe updated book and a maybe a trade."
  [{:keys [now bid-order-book] :as security-market} ask-order]
  (let [matching-order (match-order ask-order bid-order-book)]
    (cond-> security-market
      (or matching-order (cancel-immediate? bid-order-book)) (update :bid-order-book remove-order-form-book matching-order)
      matching-order (update :trades conj (order+matching-order->trade now ask-order matching-order)))))
