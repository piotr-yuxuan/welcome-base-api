(ns piotr-yuxuan.welcome-base-api.tick-test
  (:require [clojure.test :refer :all]
            [piotr-yuxuan.welcome-base-api.tick :as tick]
            [criterium.core :as c])
  (:import (java.math RoundingMode MathContext)))

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

(defn native-number-operations-perf-test
  []
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

(defn clojure-ratio-test
  []
  ;; Why using BigDecimal and ticks instead or besides powerful Clojure abstractions like Ratio?

  (c/quick-bench (/ 1 3))
  ;; => Execution time mean: 40.773048 ns
  (c/quick-bench (double (/ 1 3)))
  ;; => Execution time mean: 110.506868 ns
  (with-precision 3 :rounding RoundingMode/HALF_EVEN
    (c/quick-bench (.divide 1M 3M ^MathContext *math-context*)))
  ;; => Execution time mean: 83.785326 ns
  (with-precision 3 :rounding RoundingMode/HALF_EVEN
    (c/quick-bench (double (.divide 1M 3M ^MathContext *math-context*))))
  ;; => Execution time mean: 88.968671 ns
  )

(defn value-test
  []
  (binding [*unchecked-math* :warn-on-boxed]
    (let [bod-price 100M]
      (c/quick-bench
        ;; => Execution time mean: 138.336937 ns
        (tick/value
          (tick/size (tick/radius :percent) bod-price)
          bod-price
          101.5M)))

    (let [^int tick-radius (tick/radius :percent)
          bod-price 100M
          tick-size (tick/size tick-radius bod-price)]
      (c/quick-bench
        ;; => Execution time mean: 38.468786 ns
        (tick/value
          tick-size
          bod-price
          101.5M)))))

(defn size-perf-test
  []
  (binding [*unchecked-math* :warn-on-boxed]
    (let [^int tick-radius (tick/radius :percent)
          bod-price 100M]
      (c/quick-bench
        ;; => Execution time mean: 85.254467 ns
        (tick/size tick-radius bod-price)))))

(deftest tick-order-of-magnitude-test
  (= 2 (tick/order-of-magnitude :percent))
  (= 4
     (tick/order-of-magnitude :short)
     (tick/order-of-magnitude :anything)
     (tick/order-of-magnitude :default))
  (= 4 (tick/order-of-magnitude :ten-thousandth))
  (= 9 (tick/order-of-magnitude :integer)))

(defn ->bigdec
  ([unscaled-value] (->bigdec unscaled-value (int 0)))
  ([unscaled-value scale]
   (BigDecimal.
     (BigInteger/valueOf
       (long unscaled-value))
     (int scale))))

(defn bigdec=
  ([^BigDecimal x] (instance? BigDecimal x))
  ([^BigDecimal x ^BigDecimal y]
   (and (bigdec= x)
        (== (.unscaledValue x) (.unscaledValue y))
        (== (.scale x) (.scale y))))
  ([^BigDecimal x ^BigDecimal y & more]
   (and (bigdec= x y)
        (or (empty? more)
            (apply bigdec= y more)))))

(deftest bigdec=-test
  (is (true? (bigdec= 1M)))
  (is (true? (bigdec= 1M 1M)))
  (is (true? (bigdec= 1M 1M 1M)))
  (is (false? (bigdec= 1M 1M 1M 2M)))
  (is (false? (bigdec= 1M 1M 2M)))
  (is (false? (bigdec= 1M 2M))))

(deftest clojure-bigdec-behaviour-test
  (is (bigdec= 100M (->bigdec 100 0)))
  (is (bigdec= 100.0M (->bigdec 1000 1)))
  (is (bigdec= 100.0M (->bigdec 1000.0 1)))
  (is (bigdec= 100.0M (->bigdec 1000.0M 1)))
  (is (bigdec= 1e2M (->bigdec 1 -2)))
  (is (bigdec= 0.01M 1e-2M (->bigdec 1 2)))
  (is (not (bigdec= 100M (->bigdec 1000 1))))
  (is (= 1e2M 100M (->bigdec 1000 1) (->bigdec 1 -2)))
  (is (not (bigdec= 1e2M 100M (->bigdec 1000 1) (->bigdec 1 -2)))))

(deftest tick-size-test

  (is (bigdec= (tick/size (int 100) (->bigdec 1 -2))
               (tick/size (int 100) (->bigdec 10 -1))
               (tick/size (int 100) (->bigdec 100 0))
               1M (->bigdec 1 0)))

  (testing "with explicit max precision but no non-terminating decimal expansion"
    (with-precision 10 :rounding RoundingMode/HALF_EVEN
      (is (bigdec= (tick/size (int 100) (->bigdec 1 -2))
                   (tick/size (int 100) (->bigdec 10 -1))
                   (tick/size (int 100) (->bigdec 100 0))
                   1M (->bigdec 1 0)))))

  (testing "non-terminating decimal expansion"
    (testing "without any explicit precision, fallback of max precision of its arguments"
      (is (bigdec= 1.1M (tick/size (int 95) (->bigdec 1 -2))))
      (is (bigdec= 1.1M (tick/size (int 95) (->bigdec 10 -1))))
      (is (bigdec= 1.05M (tick/size (int 95) (->bigdec 100 0))))
      (is (bigdec= 1.052632M (tick/size (int 95) (->bigdec 1e6 4)))))
    (testing "with explicit max precision"
      (with-precision 10 :rounding RoundingMode/HALF_EVEN
        (is (bigdec= (tick/size (int 95) (->bigdec 1 -2))
                     (tick/size (int 95) (->bigdec 10 -1))
                     (tick/size (int 95) (->bigdec 100 0))
                     1.052631579M)))))

  (testing "percent"
    (let [tick-size (tick/size (tick/radius :percent) 100M)]
      (is (== tick-size 1M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) 0)))

    (let [tick-size (tick/size (tick/radius :percent) 1e9M)]
      (is (== tick-size 1E+7M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) -7)))

    (let [tick-size (tick/size (tick/radius :percent) 1e-9M)]
      (is (== tick-size 1E-11M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) 11)))

    (testing "non-trivial bod-price"
      (let [tick-size (tick/size (tick/radius :percent) 123.4567M)]
        (is (== tick-size 1.234567M))
        (is (= (.precision tick-size) 7))
        (is (= (.unscaledValue tick-size) 1234567))
        (is (= (.scale tick-size) 6)))

      (let [bod-price 123.4567e9M
            tick-size (tick/size (tick/radius :percent) bod-price)]
        (is (== tick-size 1.234567e9M))
        (is (= (.precision tick-size) 7))
        (is (= (.unscaledValue tick-size) 1234567))
        (is (= (.scale tick-size) -3)))

      (let [tick-size (tick/size (tick/radius :percent) 123.4567e-9M)]
        (is (== tick-size 1.234567e-9M))
        (is (= (.precision tick-size) 7))
        (is (= (.unscaledValue tick-size) 1234567))
        (is (= (.scale tick-size) 15)))))

  (tick/size 32767 1000000000M)

  (testing "default short"
    (is (bigdec= (->bigdec 30519 0) (tick/size (tick/radius :short) 1e9M)))
    (is (bigdec= (->bigdec 30519 9) (tick/size (tick/radius :short) 1M)))
    (is (bigdec= (->bigdec 30519 18) (tick/size (tick/radius :short) 1e-9M)))
    (testing "non-trivial bod-price"
      (is (bigdec= (->bigdec 3767714 0) (tick/size (tick/radius :short) 123.4567e9M)))
      (is (bigdec= (->bigdec 3767714 9) (tick/size (tick/radius :short) 123.4567M)))
      (is (bigdec= (->bigdec 3767714 18) (tick/size (tick/radius :short) 123.4567e-9M)))))

  (testing "default short"
    (is (bigdec= (->bigdec 4656612875 10) (tick/size (tick/radius :integer) 1e9M)))
    (is (bigdec= (->bigdec 4656612875 19) (tick/size (tick/radius :integer) 1M)))
    (is (bigdec= (->bigdec 4656612875 28) (tick/size (tick/radius :integer) 1e-9M)))
    (testing "non-trivial bod-price"
      (is (bigdec= (->bigdec 5748900588 8) (tick/size (tick/radius :integer) 123.4567e9M)))
      (is (bigdec= (->bigdec 5748900588 17) (tick/size (tick/radius :integer) 123.4567M)))
      (is (bigdec= (->bigdec 5748900588 26) (tick/size (tick/radius :integer) 123.4567e-9M))))))

(deftest tick-value-test
  (let [bod-price 100M]
    (is (== 0 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 100M)))
    (is (== 1 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 101M)))
    (is (== 2 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 102M)))

    (is (== 1 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 101.4M)))
    (testing "round even number up"
      (is (== 2 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 101.5M))))
    (is (== 2 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 101.6M)))

    (is (== 2 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 102.4M)))
    (testing "round odd number up"
      (is (== 2 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 102.5M))))
    (is (== 3 (tick/value (tick/size (tick/radius :percent) bod-price) bod-price 102.6M)))

    (is (== 0 (tick/value (tick/size (tick/radius :percent) 101M) 101M 101M)))
    (is (== 1 (tick/value (tick/size (tick/radius :percent) 101M) 101M 102M)))))

(deftest market-value-test
  tick/bod-price->market-value
  tick/size->market-value)
