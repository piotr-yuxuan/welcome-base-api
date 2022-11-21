# welcome-base-api

[![Build status](https://img.shields.io/github/workflow/status/piotr-yuxuan/welcome-base-api/Walter%20CD)](https://github.com/piotr-yuxuan/welcome-base-api/actions/workflows/walter-cd.yml)
[![GitHub license](https://img.shields.io/github/license/piotr-yuxuan/welcome-base-api)](https://github.com/piotr-yuxuan/welcome-base-api/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/piotr-yuxuan/welcome-base-api)](https://github.com/piotr-yuxuan/welcome-base-api/issues)

![Non-figurative picture](./doc/social-preview.jpg)

Simple trading market place with arbitrary arithmetic precision
computations, with an emphasis on tooling.

- Build a simple simulation of stock exchanges;
- Derive metadata such as metrics, indicators, and signals;
- Use these metadata to inform trade strategies;
- Show cycles of bank accounts creating artificial transaction volume.

Disclaimer: my financial vocabulary is still very naive. But still, it
should allow to express some nice problems to solve :-)

## Features

- System desired features:
  - Horizontally scalable, and manage up to 20 years worth
    of data with no issues
  - Allow time travel
  - Be reasonably fast and efficient, but not necessary hard real time
    for analytics
  - Exposes robust, secure, well-documented RESTful API that is
    enjoyable to use and unsurprising.
- [Indicators to
  compute](https://www.investopedia.com/articles/active-trading/041814/four-most-commonlyused-indicators-trend-trading.asp):
  - [CAC40 index](https://en.wikipedia.org/wiki/CAC_40)
  - Moving averages (pre-defined at first, and then any sliding windows)
  - Exponential moving average (emphasis put on recent prices)
  - Detect trading range (a more or less narrow price range in which
    a security trades for some time)
  - Buy and sell signals (relative position of 200-day and 50-day
    moving averages)
  - [Volatility](https://en.wikipedia.org/wiki/Volatility_(finance))
  - Client sentiment (the proportion selling or buying)
- Query features:
  - Historical, Godlike value, knowing everything anyywhere
  - As with the knowledge of some specific trader as of some (consume)
    time

# Entities and verbs

- An
  [exchange](https://en.wikipedia.org/wiki/Exchange_(organized_market))
  is an organized market where securities are bought and sold.
- An [order
  matcher](https://en.wikipedia.org/wiki/Order_matching_system)
  executes orders, it's the core of the exchange.
- A [security](https://en.wikipedia.org/wiki/Security_(finance)) is a
  tradable financial asset.
- The [spot market](https://en.wikipedia.org/wiki/Spot_market) is
  exchanges in which securities are traded for immediate delivery, not
  as options.
- [Market
  capitalisation](https://en.wikipedia.org/wiki/Market_capitalization)
  is the total value of outstanding shares
- Accounts
- Dividends
- A
  [ticker](https://www.londonstockexchange.com/help/whats-ftse-index-overview#what-is-ticker-)
- https://medium.com/lgogroup/a-matching-engine-for-our-values-part-1-795a29b400fa
- https://blog.kaiko.com/tick-level-order-books-technical-overview-and-documentation-56b1ab6e7c10
- See FIX protocol https://github.com/alexkachanov/simpleFIXClient

# Local definitions

- Companies are listed on some exchanges
- Companies issues shares and distribute dividends
- An exchange gets fees on listing and transactions
- An exchange can suspend listing of a company share if the sell / buy
  ratio becomes too imbalanced
- Traders buy or sell shares, and get fees for that
- Shareholders receive dividends from the company
