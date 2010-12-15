package com.jayway.restassured.assertion

class BodyMatcherGroup {
  private List bodyAssertions = []
  def leftShift(Object bodyMatcher) {
    bodyAssertions << bodyMatcher
  }

  def isFulfilled(response, content) {
    def treatedContent
    if(content instanceof InputStreamReader) {
      treatedContent = content.readLines().join()
    } else {
      treatedContent = content
    }

    bodyAssertions.each { assertion ->
        assertion.isFulfilled(response, treatedContent)
    }
  }

  def boolean requiresContentTypeText() {
    def numberOfRequires = 0
      def numberOfNonRequires = 0
      bodyAssertions.each { matcher ->
        if(matcher.requiresContentTypeText()) {
          numberOfRequires++
        } else {
          numberOfNonRequires++
        }
      }
      throwExceptionIfIllegalBodyAssertionCombinations(numberOfRequires, numberOfNonRequires)

      return numberOfRequires != 0
  }

  def String getDescriptions() {
    String descriptions = ""
    bodyAssertions.each {
      descriptions = it.getDescription()
    }
    return descriptions
  }

  private def throwExceptionIfIllegalBodyAssertionCombinations(int numberOfRequires, int numberOfNonRequires) {
    if (numberOfRequires > 0 && numberOfNonRequires > 0) {
      String matcherDescription = "";
      bodyAssertions.each { matcher ->
        def String hamcrestDescription = matcher.getDescription()
        matcherDescription += "\n$hamcrestDescription "
        if (matcher.requiresContentTypeText()) {
          matcherDescription += "which requires 'TEXT'"
        } else {
          matcherDescription += "which cannot be 'TEXT'"
        }
      }
      throw new IllegalStateException("""Currently you cannot mix body expectations that require different content types for matching.
For example XPath and full body matching requires TEXT content and JSON/XML matching requires JSON/XML/ANY mapping. You need to split conflicting matchers into two tests. Your matchers are:$matcherDescription""")
    }
  }
}
