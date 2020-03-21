package com.ruchij

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ExampleTest extends AnyFlatSpec with Matchers {
  "Sample test" should "pass nicely" in {
    8 mustBe 8
  }
}
