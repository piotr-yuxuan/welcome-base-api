# HTTP

## Headers

- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Location
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Link
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Date
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Downlink
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Expect-CT
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/From
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Match
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Match
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-None-Match
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Range
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Unmodified-Since
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Keep-Alive
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Last-Modified
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/WWW-Authenticate

## Status codes

- https://en.wikipedia.org/wiki/List_of_HTTP_status_codes

# Engineering, theory, practice, and ladder

- https://codecapsule.com/2021/07/15/the-skills-map-of-senior-tech-career-progression/
- https://db.in.tum.de/~leis/papers/ART.pdf
- https://docs.xtdb.com/clients/clojure/
- https://geekflare.com/time-series-database/
- https://github.com/alimpfard/mal/blob/q/impls/q/core.q
- https://medium.com/@bcarunmail/securing-rest-api-using-keycloak-and-spring-oauth2-6ddf3a1efcc2
- https://medium.com/lgogroup/a-matching-engine-for-our-values-part-1-795a29b400fa
- https://medium.com/@yy2799/realtime-financial-market-data-visualization-and-analysis-using-kafka-cassandra-and-bokeh-eac22139e5
- https://nextjournal.com/xtdb-tutorial/put
- https://pragprog.com/titles/bhwb/exercises-for-programmers/
- https://r.bluethl.net/how-to-design-better-apis
- https://timelydataflow.github.io/differential-dataflow/
- https://tomgamon.com/posts/things-i-have-learned-new-tech-lead/
- https://xtdb.com/
- https://zipkin.io/pages/instrumenting.html

## API

- https://clojure-liberator.github.io/liberator/assets/img/decision-graph.svg
- https://www.youtube.com/watch?v=0V_F9Ko_Ic8 Discoverable Hypermedia-Driven RESTful Web Services in Clojure - Daniel Zurawski
- https://www.youtube.com/watch?v=JWa4NhjWNHQ Building a RESTful Web API in Clojure - a new approach - Malcolm Sparks
- https://www.rest.guide/README.html
- https://github.com/jimmythompson/halboy
- https://github.com/b-social/hype

## JVM

- https://en.wikipedia.org/wiki/JDK_Flight_Recorder

## Data structures

- https://stratos.seas.harvard.edu/files/stratos/files/periodictabledatastructures.pdf
- https://en.wikipedia.org/wiki/Y-fast_trie Quite amazing to find the
  closest order of a price in `O(log log M)` in `O(n)` space, with `M`
  the max possible value. Sounds quite appealing not to depend of the
  number of entries. Knowing that some market regulations forbid some
  price variation bigger than 10%, we can just allocate enough ticks
  to allow these moves. Also, a
  [tick](https://en.wikipedia.org/wiki/Tick_size) can be defined in
  relation to the current price, which means there is a constant
  precision that doesn't change depending on the price facial
  value. All this sounds quite great!
- Concurrent Y-fast trie: SkipTrie.
  - http://aleksandar-prokopec.com/resources/docs/p137-prokopec.pdf
  - https://groups.csail.mit.edu/mag/oshman-shavit-podc13.pdf
  - https://github.com/JoeLeavitt/SkipTrie
- If not applicable, https://en.wikipedia.org/wiki/AVL_tree a simple
  AVL tree offers `Θ(log n)` search in `O(n)` space.
- https://opendatastructures.org/ods-java/13_3_YFastTrie_Doubly_Logar.html

## Clojure

- https://practical.li/
- https://www.youtube.com/watch?v=bQDkuF6-py4
- https://github.com/metosin/talvi
- https://bsless.github.io/code-smells/
- https://www.clojure-toolbox.com/
- https://github.com/oxalorg/clj-konmari

### Performance

- https://www.youtube.com/watch?v=3SSHjKT3ZmA Naked Performance (with Clojure)
- https://www.youtube.com/watch?v=5BKn4rUoggg (!) Dressed up performance in Clojure
- https://www.youtube.com/watch?v=5mUGu4RlwKE High Performance Data With Clojure (! from an NVidia alumni, talking about array programming)
- https://www.youtube.com/watch?v=_6qVfFkBdWI Performance testing with JMH Clojure & Java Microbenchmark Harness
- https://github.com/bsless/clj-fast
- https://github.com/clojure/data.int-map
- http://clojure-goes-fast.com/
- https://github.com/techascent/tech.ml.dataset with parquet files
- https://github.com/metosin/pohjavirta (look at the JVM opts)
- https://github.com/bsless/stress-server
- https://bsless.github.io/datahike-datalog-parser/
- https://bsless.github.io/fast-and-elegant-clojure/

# Finance

- https://blog.kaiko.com/tick-level-order-books-technical-overview-and-documentation-56b1ab6e7c10
- https://en.wikipedia.org/wiki/Best_execution
- https://en.wikipedia.org/wiki/Bid%E2%80%93ask_matrix
- https://en.wikipedia.org/wiki/Bid%E2%80%93ask_spread
- https://en.wikipedia.org/wiki/Dark_pool
- https://en.wikipedia.org/wiki/Exchange_(organized_market)
- https://en.wikipedia.org/wiki/Finance
- https://en.wikipedia.org/wiki/Financial_market
- https://en.wikipedia.org/wiki/Jump_point_search
- https://en.wikipedia.org/wiki/List_of_electronic_trading_protocols#Europe
- https://en.wikipedia.org/wiki/London_Stock_Exchange#Primary_markets
- https://en.wikipedia.org/wiki/Mid_price
- https://en.wikipedia.org/wiki/Order_(exchange)
- https://en.wikipedia.org/wiki/Order_book
- https://en.wikipedia.org/wiki/Order_matching_system
- https://en.wikipedia.org/wiki/Settlement_(finance)
- https://en.wikipedia.org/wiki/Share_(finance)#Valuation
- https://en.wikipedia.org/wiki/Short_(finance)
- https://en.wikipedia.org/wiki/Solvency_cone
- https://github.com/eliquinox/jLOB
- https://github.com/henryw374/tools.jvm
- https://github.com/mbuczko/cerber-oauth2-provider#api
- https://github.com/mzheravin/exchange-core
- https://lambdaisland.com/blog/2022-03-10-mechanism-vs-policy
- https://lawecommons.luc.edu/cgi/viewcontent.cgi?article=1025&context=luclj

## Data

- https://www.alphavantage.co/
- https://marketstack.com/
- https://finnhub.io/pricing

## API

- https://kite.trade
