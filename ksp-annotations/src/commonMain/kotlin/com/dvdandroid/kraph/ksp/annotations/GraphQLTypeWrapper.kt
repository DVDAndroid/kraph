package com.dvdandroid.kraph.ksp.annotations

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class GraphQLTypeWrapper(
  val outClassName: String,
  val outFieldName: String,
)