(ns piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo-test
  (:require [clojure.test :refer :all]
            [piotr-yuxuan.welcome-base-api.order-matcher.simple-fifo :as simple-fifo]))

(deftest tick-order-of-magnitude-test
  (= 2 (simple-fifo/tick-order-of-magnitude :percent))
  (= 4
     (simple-fifo/tick-order-of-magnitude :short)
     (simple-fifo/tick-order-of-magnitude :anything)
     (simple-fifo/tick-order-of-magnitude :default))
  (= 4 (simple-fifo/tick-order-of-magnitude :ten-thousandth))
  (= 9 (simple-fifo/tick-order-of-magnitude :integer)))

(deftest tick-size-test
  (testing "percent"
    (let [tick-size (simple-fifo/tick-size :percent 100M)]
      (is (== tick-size 1M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) 0)))

    (let [tick-size (simple-fifo/tick-size :percent 1e9M)]
      (is (== tick-size 1E+7M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) -7)))

    (let [tick-size (simple-fifo/tick-size :percent 1e-9M)]
      (is (== tick-size 1E-11M))
      (is (= (.precision tick-size) 1))
      (is (= (.unscaledValue tick-size) 1))
      (is (= (.scale tick-size) 11)))

    (testing "non-trivial bod-price"
      (let [tick-size (simple-fifo/tick-size :percent 123.4567M)]
        (is (== tick-size 1.23M))
        (is (= (.precision tick-size) 3))
        (is (= (.unscaledValue tick-size) 123))
        (is (= (.scale tick-size) 2)))

      (let [tick-size (simple-fifo/tick-size :percent 123.4567e9M)]
        (is (== tick-size 123e7M))
        (is (= (.precision tick-size) 3))
        (is (= (.unscaledValue tick-size) 123))
        (is (= (.scale tick-size) -7)))

      (let [tick-size (simple-fifo/tick-size :percent 123.4567e-9M)]
        (is (== tick-size 123E-11M))
        (is (= (.precision tick-size) 3))
        (is (= (.unscaledValue tick-size) 123))
        (is (= (.scale tick-size) 11)))))

  (testing "default short"
    (let [tick-size (simple-fifo/tick-size :short 1e9M)]
      (is (== tick-size 30519M))
      (is (= (.precision tick-size) 5))
      (is (= (.unscaledValue tick-size) 30519))
      (is (= (.scale tick-size) 0)))

    (let [tick-size (simple-fifo/tick-size :short 1M)]
      (is (== tick-size 0.000030519M))
      (is (= (.precision tick-size) 5))
      (is (= (.unscaledValue tick-size) 30519))
      (is (= (.scale tick-size) 9)))

    (let [tick-size (simple-fifo/tick-size :short 1e-9M)]
      (is (== tick-size 3.0519E-14M))
      (is (= (.precision tick-size) 5))
      (is (= (.unscaledValue tick-size) 30519))
      (is (= (.scale tick-size) 18)))

    (testing "non-trivial bod-price"
      (let [tick-size (simple-fifo/tick-size :short 123.4567e9M)]
        (is (== tick-size 3.7677e6M))
        (is (= (.precision tick-size) 5))
        (is (= (.unscaledValue tick-size) 37677))
        (is (= (.scale tick-size) -2)))

      (let [tick-size (simple-fifo/tick-size :short 123.4567M)]
        (is (== tick-size 0.0037677M))
        (is (= (.precision tick-size) 5))
        (is (= (.unscaledValue tick-size) 37677))
        (is (= (.scale tick-size) 7)))

      (let [tick-size (simple-fifo/tick-size :short 123.4567e-9M)]
        (is (== tick-size 3.7677e-12M))
        (is (= (.precision tick-size) 5))
        (is (= (.unscaledValue tick-size) 37677))
        (is (= (.scale tick-size) 16))))))

(deftest tick-value-test
  (is (== 0 (simple-fifo/bod-price+market-value->tick-value :percent 100M 100M)))
  (is (== 1 (simple-fifo/bod-price+market-value->tick-value :percent 100M 101M)))
  (is (== 2 (simple-fifo/bod-price+market-value->tick-value :percent 100M 102M)))

  (is (== 1 (simple-fifo/bod-price+market-value->tick-value :percent 100M 101.4M)))
  (testing "round even number up"
    (is (== 2 (simple-fifo/bod-price+market-value->tick-value :percent 100M 101.5M))))
  (is (== 2 (simple-fifo/bod-price+market-value->tick-value :percent 100M 101.6M)))

  (is (== 2 (simple-fifo/bod-price+market-value->tick-value :percent 100M 102.4M)))
  (testing "round odd number up"
    (is (== 2 (simple-fifo/bod-price+market-value->tick-value :percent 100M 102.5M))))
  (is (== 3 (simple-fifo/bod-price+market-value->tick-value :percent 100M 102.6M)))

  (is (== 0 (simple-fifo/bod-price+market-value->tick-value :percent 101M 101M)))
  (is (== 1 (simple-fifo/bod-price+market-value->tick-value :percent 101M 102M))))

(deftest market-value-test
  simple-fifo/bod-price+tick-value->market-value
  simple-fifo/tick-size+tick-value->market-value)
