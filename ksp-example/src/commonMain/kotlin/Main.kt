@file:Suppress("unused")

import com.dvdandroid.kraph.ksp.annotations.*
import com.test.AddressGraphQLBuilder.Companion.address
import com.test.UserGraphQLBuilder.Companion.user
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
@GraphQLTypeWrapper(outClassName = "UserGraphQL", outFieldName = "users")
data class User(
  val id: String,
  val name: String,
  val email: MutableSet<String>,
  val address: Address,
)

@GraphQLType
data class Address(
  val city: String,
  val cap: Int,
  @GraphQLFieldIgnore
  val test: String?
)

@GraphQLInputType
data class InputUser(
  val name: String,
  val email: String,
  val test: String? = null,
  @GraphQLInputFieldIgnore
  val x: String = "X",
)