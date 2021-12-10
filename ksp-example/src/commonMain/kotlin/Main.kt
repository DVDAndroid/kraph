@file:Suppress("ClassName")

import AddressGraphQLBuilder.Companion.address
import UserGraphQLBuilder.Companion.user
import annotations.GraphQLType
import me.lazmaid.kraph.Kraph

fun example(): String = Kraph {
  query {
    user {
      id
      address {
        city
      }
      field("test")
    }
    address {
      city
    }
  }
}.toGraphQueryString()

@GraphQLType
data class User(
  val id: String,
  val name: String,
  val email: String,
  val address: Address,
)

@GraphQLType
data class Address(
  val city: String,
  val cap: Int,
)