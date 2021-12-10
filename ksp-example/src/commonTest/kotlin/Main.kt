import kotlin.test.Test
import kotlin.test.assertEquals

class Test {

  @Test
  fun run() {
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

}