@file:Suppress("unused")

//fun example(): String = Kraph {
//  query {
//    user {
//      id
//      address {
//        city
//      }
//      field("test")
//    }
//    address {
//      city
//    }
//  }
//}.toGraphQueryString()
//
//@GraphQLType
//data class User(
//  val id: String,
//  val name: String,
//  val email: MutableSet<String>,
//  val address: Address,
//)
//
//@GraphQLType
//data class Address(
//  val city: String,
//  val cap: Int,
//)
//
//@GraphQLInputType
//data class InputUser(
//  val name: String,
//  val email: String,
//  val test: String? = null,
//  @GraphQLFieldIgnore
//  val x: String = "X",
//)