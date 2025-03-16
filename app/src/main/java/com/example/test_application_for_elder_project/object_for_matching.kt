package com.example.test_application_for_elder_project

data class ObjectForMatching(
    var email: String = "",
    var interests: MutableList<String> = mutableListOf(),
    var name: String = "",
    var password: String = "",
    var role: String = "",
    var timeSignedIn: String = ""
) {
    // No-argument constructor required by Firestore
    constructor() : this("", mutableListOf(), "", "", "", "")
}