import kotlin.test.Test
import kotlin.test.assertEquals

class Test {

  @Test
  fun builder() {
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
}""", actual = example())
  }

  @Test
  fun asHashMap() {
    assertEquals(
      actual = hashMapOf(
        "name" to "name",
        "email" to "email",
      ),
      expected = InputUser("name", "email").asHashMap(),
    )
  }

}