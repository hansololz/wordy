package com.deezus.wordy.helpers


class Tuple2<T1: Any, T2: Any>(val _1: T1, val _2: T2) {
  fun <T3: Any>thenLet(callback: (T1, T2) -> T3?): T3? {
    return callback.invoke(_1, _2)
  }
}

class Tuple3<T1: Any, T2: Any, T3: Any>(val _1: T1, val _2: T2, val _3: T3) {
  fun <T4: Any>thenLet(callback: (T1, T2, T3) -> T4?): T4? {
    return callback.invoke(_1, _2, _3)
  }
}

class Tuple4<T1: Any, T2: Any, T3: Any, T4: Any>(val _1: T1, val _2: T2, val _3: T3, val _4: T4) {
  fun <T5: Any>thenLet(callback: (T1, T2, T3, T4) -> T5?): T5? {
    return callback.invoke(_1, _2, _3, _4)
  }
}

class Tuple5<T1: Any, T2: Any, T3: Any, T4: Any, T5: Any>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5) {
  fun <T6: Any>thenLet(callback: (T1, T2, T3, T4, T5) -> T6?): T6? {
    return callback.invoke(_1, _2, _3, _4, _5)
  }
}

fun <T1: Any, T2: Any> given(p1: T1?, p2: T2?): Tuple2<T1, T2>? {
  if (p1 != null && p2 != null) {
    return Tuple2(p1, p2)
  }

  return null
}

fun <T1: Any, T2: Any, T3: Any> given(p1: T1?, p2: T2?,  p3: T3?): Tuple3<T1, T2, T3>? {
  if (p1 != null && p2 != null && p3 != null) {
    return Tuple3(p1, p2, p3)
  }

  return null
}

fun <T1: Any, T2: Any, T3: Any, T4: Any> given(p1: T1?, p2: T2?, p3: T3?, p4: T4?): Tuple4<T1, T2, T3, T4>? {
  if (p1 != null && p2 != null && p3 != null && p4 != null) {
    return Tuple4(p1, p2, p3, p4)
  }

  return null
}

fun <T1: Any, T2: Any, T3: Any, T4: Any, T5: Any> given(p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?): Tuple5<T1, T2, T3, T4, T5>? {
  if (p1 != null && p2 != null && p3 != null && p4 != null  && p5 != null) {
    return Tuple5(p1, p2, p3, p4, p5)
  }

  return null
}