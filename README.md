# Podcast API Scala Library

The Podcast API Scala library provides convenient access to the [Listen Notes Podcast API](https://www.listennotes.com/api/) from
applications written in the Scala language.

Simple and no-nonsense podcast search & directory API. Search the meta data of all podcasts and episodes by people, places, or topics. It's the same API that powers [the best podcast search engine Listen Notes](https://www.listennotes.com/).

This repo is actually a demo app using [the podcast-api Java library](https://github.com/ListenNotes/podcast-api-java).
You can find example Scala code in the README.md of this repo.

If you have any questions, please contact [hello@listennotes.com](hello@listennotes.com?subject=Questions+about+the+Scala+SDK+of+Listen+API)

<a href="https://www.listennotes.com/api/"><img src="https://raw.githubusercontent.com/ListenNotes/ListenApiDemo/master/web/src/powered_by_listennotes.png" width="300" />


**Table of Contents**


## Installation

You can install this library for JVM-based languages, including Java, Kotlin, Clojure, Scala, Groovy...

### Gradle users

Add this dependency to your project's build file:

```groovy
implementation "com.listennotes:podcast-api:1.0.6"
```

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
  <groupId>com.listennotes</groupId>
  <artifactId>podcast-api</artifactId>
  <version>1.0.6</version>
</dependency>
```

## Usage

The library needs to be configured with your account's API key which is
available in your [Listen API Dashboard](https://www.listennotes.com/api/dashboard/#apps). Set `apiKey` to its
value:

```scala
import com.listennotes.podcast_api.Client
import com.listennotes.podcast_api.exception._

object App {
  def main(args: Array[String]): Unit = {
    // If apiKey is not set or an empty string, then we'll connect
    // to the api mock server, which returns fake data for testing
    val apiKey = scala.util.Properties.envOrElse("LISTEN_API_KEY", "")
    val client = new Client(apiKey)

    // Parameters are passed via this HashMap
    // All parameters can be found at:
    //     https://www.listennotes.com/api/docs/
    val parameters = new java.util.HashMap[String, String]
    parameters.put("q", "startup")
    parameters.put("type", "episode")    
    parameters.put("sort_by_date", "1")    

    try {
      val response = client.search(parameters)

      // response.toJSON() returns an org.json.JSONObject
      println(response.toJSON().toString(2))

      println("\n=== Some stats of your account ===\n")
      println("Free Quota this month: " + response.getFreeQuota() + " requests")
      println("Usage this month: " + response.getUsage() + " requests")    
    } catch {
      case e: AuthenticationException => println("Wrong API key")
      case e: RateLimitException => println("you are using FREE plan and you exceed the quota limit")
    }
  }
}
```

If `apiKey` is null or "", then we'll connect to a [mock server](https://www.listennotes.com/api/tutorials/#faq0) that returns fake data for testing purposes.

You can quickly run sample code using gradle:
```shell
# Use api mock server for test data
./gradlew run

# Use production server for real data
LISTEN_API_KEY=your-api-key-here ./gradlew run
```


### Handling exceptions

Unsuccessful requests raise exceptions. The class of the exception will reflect
the sort of error that occurred.

| Exception Class  | Description |
| ------------- | ------------- |
|  AuthenticationException | wrong api key or your account is suspended  |
| ApiConnectionException  | fail to connect to API servers  |
| InvalidRequestException  | something wrong on your end (client side errors), e.g., missing required parameters  |
| RateLimitException  | you are using FREE plan and you exceed the quota limit  |
| NotFoundException  | endpoint not exist, or podcast / episode not exist  |
| ListenApiException  | something wrong on our end (unexpected server errors)  |

All exception classes can be found in [this folder](https://github.com/ListenNotes/podcast-api-java/tree/main/src/main/java/com/listennotes/podcast_api/exception).

And you can see some sample code [here](https://github.com/ListenNotes/podcast-api-scala/blob/main/app/src/main/scala/podcast/api/scala/demo/App.scala).
