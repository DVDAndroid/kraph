import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleTest {

  @Test
  fun builder() {
    //language=GraphQL
    assertEquals(expected = """query {
  user {
    id
    address {
      city
    }
    test
  }
  address {
    city
  }
  devices {
    ... on AndroidDevice {
      model
    }
  }
}""", actual = example())
  }

//  @Test
//  fun asHashMap() {
//    assertEquals(
//      actual = hashMapOf(
//        "name" to "name",
//        "email" to "email",
//        "number" to "123",
//      ),
//      expected = InputUser("name", "email").asHashMap(),
//    )
//  }

}