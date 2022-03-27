package com.deezus.wordy.helpers


//fun setSwitchController(
//  baseActivity: BaseActivity,
//  switch: SwitchCompat,
//  enumSetting: EnumSetting,
//  onValue: DefinedValue = DefinedValue.ON,
//  offValue: DefinedValue = DefinedValue.OFF,
//  callback: () -> Unit = {}) {
//
//  switch.isChecked = enumSetting.getValue(baseActivity) == onValue
//
//  switch.setOnClickListener {
//    if (switch.isChecked) {
//      enumSetting.setValue(baseActivity, onValue)
//      callback()
//    } else {
//      enumSetting.setValue(baseActivity, offValue)
//      callback()
//    }
//  }
//}
//
//fun setRadioButtonController(baseActivity: BaseActivity, button: RadioButton, enumSetting: EnumSetting, value: DefinedValue, callback: ((Boolean) -> Unit)? = null) {
//  button.isChecked = enumSetting.getValue(baseActivity) == value
//
//  button.setOnClickListener {
//    val oldValue = enumSetting.getValue(baseActivity)
//
//    enumSetting.setValue(baseActivity, value)
//
//    callback?.invoke(oldValue != value)
//  }
//}