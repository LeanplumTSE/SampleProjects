//
//  UserModel.swift
//  swiftExample
//
//  Created by Kyle Cote on 12/8/16.
//  Copyright © 2016 Leanplum. All rights reserved.
//

import Foundation

/**
* I created a simple User based model that keeps track of user information.
* This would be specific user information and is mainly for demonstrating logged in functionality.
*/
class UserModel{
    
    private var m_isPushEnabled = false
    private var m_userId = ""
    
    var userId: String{
        get{
            return m_userId
        }
        set{
            m_userId = newValue
        }
    }
    
    
    var pushIsEnabled: Bool{
        get {
            return m_isPushEnabled
        }
        set {
            m_isPushEnabled = newValue
        }
    }
    
}
