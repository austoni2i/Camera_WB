package com.app.camerawb

class ColorTool {


    var red = 0
        get() = field
        set(value) {field = value}
    var green = 0
        get() = field
        set(value) { field = value}
    var blue = 0
        get() = field
        set(value) { field = value}
    var magenta = 0
        get() = field
        set(value) { field = value}
    var hue = 0
        get() = field
        set(value) { field = value}
    var contrast = 0
        get() = field
        set(value) { field = value}
    var brightness = 0
        get() = field
        set(value) { field = value}
    var saturation = 0
        get() = field
        set(value) { field = value}
    var ruleString = ""
        get() = field
        set(value) {
            field = value
        }

    fun reset(){
        red = 0
        green = 0
        blue = 0
        magenta = 0
        hue = 0 //0 Default
        contrast = 1 //1 Default
        brightness = 0 //0 Default
        saturation = 1 //1 Default
    }

}