(ns piotr-yuxuan.welcome-base-api.tick
  "This should be so simple it's can't be wrong, but nonetheless well
  tested. Then it may serve as a reference implementation for later
  more optimised strategies."
  (:import (java.math RoundingMode MathContext)))

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

(def short-radius
  Short/MAX_VALUE)

(def market-type->value-radius
  "As ticks are statically expressed with integers, we can't go more
  precise that `Integer/MAX_VALUE`. The precision increase the cost of
  lookups in Y-fast tries."
  {:percent 100
   :ten-thousandth 1e4
   :short short-radius
   :integer Integer/MAX_VALUE})

(defn radius
  [market-type]
  "Centered in 0. Set it to `[-100, 100]` for a percent precision. It
  might not be necessary be as large value as Short span."
  (int (get market-type->value-radius market-type short-radius)))

(defn value-center
  [_market-type]
  "By definition it is zero."
  0)

(defn order-of-magnitude
  [market-type]
  "The smallest order of magnitude that can be represented in tick
  domain. Use one more (`inc`) to make sure you lose no information.

  ``` clojure
  (binding [*unchecked-math* :warn-on-boxed]
    (with-precision (inc (tick-order-of-magnitude market-type)) :rounding RoundingMode/HALF_EVEN
      ;; some operations
      ))
  ```"
  (->> (radius market-type)
       Math/log10
       Math/floor
       Math/round
       int))

(defn precision
  [market-type]
  "Tick precision is a ratio. Tick size is relative to the market value at the beginning of day."
  (/ 1 (radius market-type)))

(def bod-ticks
  "By definition. tick-value can vary within `(tick-value-span market-type)`."
  (int 0))

(defn ^BigDecimal size
  "Run once at beginning of day, so we may create a transient MathContext on every call. No more precise than `bod-price`."
  [tick-radius ^BigDecimal bod-price]
  (let [precision (->> (Math/log10 tick-radius) (Math/ceil) (max (.precision bod-price)))
        math-context (or *math-context*
                         (MathContext. precision
                                       RoundingMode/HALF_EVEN))]
    (.divide bod-price
             (BigDecimal. ^int tick-radius)
             ^MathContext math-context)))

(defn value
  [^BigDecimal tick-size ^BigDecimal bod-price ^BigDecimal market-value]
  (-> (.subtract market-value bod-price)
      (.divide tick-size 0 RoundingMode/HALF_EVEN)
      ;; Immediate because scale = 0.
      .intValue))

(defn ^BigDecimal bod-price->market-value
  [^BigDecimal tick-size ^BigDecimal bod-price tick-value]
  (.add (.multiply tick-size tick-value) bod-price))

(defn ^BigDecimal size->market-value
  [tick-size tick-value]
  (BigDecimal. ^int (* tick-size tick-value)))

;; A tick is just a number, it is not money. We take the price at the
;; beginning of day (bod-price) and span a range of integers from -1e3
;; to 1e3 that represents all the possible price values around ±10% of
;; bod-price. This means that the actual price of a deal is completely
;; defined by the bod-price of the market and a tick.
