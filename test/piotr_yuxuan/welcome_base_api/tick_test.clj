(ns piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo-test
  (:require [clojure.test :refer :all]
            [piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo :as simple-fifo])
  (:import (java.math RoundingMode)))

(deftest tick-order-of-magnitude-test
  (= 2 (simple-fifo/tick-order-of-magnitude :percent))
  (= 4
     (simple-fifo/tick-order-of-magnitude :short)
     (simple-fifo/tick-order-of-magnitude :anything)
     (simple-fifo/tick-order-of-magnitude :default))
  (= 4 (simple-fifo/tick-order-of-magnitude :ten-thousandth))
  (= 9 (simple-fifo/tick-order-of-magnitude :integer)))

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
   (and (instance? BigDecimal x)
        (instance? BigDecimal y)
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

(->> (Math/log10 (->bigdec 100 0)) (Math/ceil) int
     ;; One more to make sure we lose nothing?
     ;unchecked-inc-int
     )

(deftest tick-size-test

  (is (bigdec= (simple-fifo/tick-radius+bod-price->tick-size (int 100) (->bigdec 1 -2))
               (simple-fifo/tick-radius+bod-price->tick-size (int 100) (->bigdec 10 -1))
               (simple-fifo/tick-radius+bod-price->tick-size (int 100) (->bigdec 100 0))
               1M (->bigdec 1 0)))

  (testing "with explicit max precision but no non-terminating decimal expansion"
    (with-precision 10 :rounding RoundingMode/HALF_EVEN
      (is (bigdec= (simple-fifo/tick-radius+bod-price->tick-size (int 100) (->bigdec 1 -2))
                   (simple-fifo/tick-radius+bod-price->tick-size (int 100) (->bigdec 10 -1))
                   (simple-fifo/tick-radius+bod-price->tick-size (int 100) (->bigdec 100 0))
                   1M (->bigdec 1 0)))))

  (testing "non-terminating decimal expansion"
    (testing "without any explicit precision, fallback of max precision of its arguments"
      (is (bigdec= 1.1M (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 1 -2))))
      (is (bigdec= 1.1M (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 10 -1))))
      (is (bigdec= 1.05M (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 100 0))))
      (is (bigdec= 1.052632M (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 1e6 4)))))
    (testing "with explicit max precision"
      (with-precision 10 :rounding RoundingMode/HALF_EVEN
        (is (bigdec= (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 1 -2))
                     (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 10 -1))
                     (simple-fifo/tick-radius+bod-price->tick-size (int 95) (->bigdec 100 0))
                     1.052631579M)))))

  (testing "percent"
    (let [tick-size (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 100M)]
      (is (== tick-size 1M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) 0)))

    (let [tick-size (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 1e9M)]
      (is (== tick-size 1E+7M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) -7)))

    (let [tick-size (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 1e-9M)]
      (is (== tick-size 1E-11M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) 11)))

    (testing "non-trivial bod-price"
      (let [tick-size (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 123.4567M)]
        (is (== tick-size 1.234567M))
        (is (= (.precision tick-size) 7))
        (is (= (.unscaledValue tick-size) 1234567))
        (is (= (.scale tick-size) 6)))

      (let [bod-price 123.4567e9M
            tick-size (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price)]
        (is (== tick-size 1.234567e9M))
        (is (= (.precision tick-size) 7))
        (is (= (.unscaledValue tick-size) 1234567))
        (is (= (.scale tick-size) -3)))

      (let [tick-size (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 123.4567e-9M)]
        (is (== tick-size 1.234567e-9M))
        (is (= (.precision tick-size) 7))
        (is (= (.unscaledValue tick-size) 1234567))
        (is (= (.scale tick-size) 15)))))

  (simple-fifo/tick-radius+bod-price->tick-size 32767 1000000000M)

  (testing "default short"
    (is (bigdec= (->bigdec 30519 0) (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :short) 1e9M)))
    (is (bigdec= (->bigdec 30519 9) (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :short) 1M)))
    (is (bigdec= (->bigdec 30519 18) (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :short) 1e-9M)))
    (testing "non-trivial bod-price"
      (is (bigdec= (->bigdec 3767714 0) (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :short) 123.4567e9M)))
      (is (bigdec= (->bigdec 3767714 9) (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :short) 123.4567M)))
      (is (bigdec= (->bigdec 3767714 18) (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :short) 123.4567e-9M))))))

(deftest tick-value-test
  (let [bod-price 100M]
    (is (== 0 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 100M)))
    (is (== 1 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 101M)))
    (is (== 2 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 102M)))

    (is (== 1 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 101.4M)))
    (testing "round even number up"
      (is (== 2 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 101.5M))))
    (is (== 2 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 101.6M)))

    (is (== 2 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 102.4M)))
    (testing "round odd number up"
      (is (== 2 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 102.5M))))
    (is (== 3 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) bod-price) bod-price 102.6M)))

    (is (== 0 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 101M) 101M 101M)))
    (is (== 1 (simple-fifo/bod-price+market-value->tick-value (simple-fifo/tick-radius+bod-price->tick-size (simple-fifo/tick-radius :percent) 101M) 101M 102M)))))

(deftest market-value-test
  simple-fifo/bod-price+tick-value->market-value
  simple-fifo/tick-size+tick-value->market-value)
