package com.shohiebsense.firebasenosqlsampleapp

class ChatMessage() {

    var id = ""
    var text = ""
    var name = ""
    var photoUrl = ""
    var imageUrl = ""

    constructor(text: String, name: String, photoUrl: String, imageUrl: String) :  this(){
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
        this.imageUrl = imageUrl
    }

}