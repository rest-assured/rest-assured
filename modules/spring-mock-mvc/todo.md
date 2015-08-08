* Add webappSetup and standaloneSetup to MockSpecBuilder
* Test: 
  static: RestAssured.webappSetup(context)
  test1: given().postProcessor(postProcessor1)
  test2: Don't apply any post processor. Make sure that postProcessor1 is not applied in test2 
