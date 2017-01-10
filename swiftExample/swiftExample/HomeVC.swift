//
//  HomeVC.swift
//  swiftExample
//
//  Created by Kyle Cote on 12/8/16.
//  Copyright © 2016 Leanplum. All rights reserved.
//

import UIKit
import Leanplum

class HomeVC: UIViewController {
    
    var m_user: UserModel? = nil
    let defaultUserdIdText = "HauteLookUser"
    let pushEnabledText = "Enabled"
    let pushDiabledText = "Disabled"
    
    

    @IBOutlet weak var userIdValue: UITextField!
    @IBOutlet weak var userIdDisplayValue: UILabel!
    
    @IBOutlet weak var ageTextField: UITextField!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        userIdValue.text = defaultUserdIdText
        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    @IBAction func loginToApplication(_ sender: UIButton) {
        
        //Init the userID if necessary
        if(m_user == nil){
            m_user = UserModel()
        }

        if let newUserId = userIdValue.text{
            
            //Set the UserID & call Leanplum to notify the userId change.
            m_user?.userId = newUserId
            Leanplum.setUserId(newUserId)
            userIdDisplayValue.text = newUserId
        }
    }
    
    @IBAction func setUserAttributes(_ sender: UIButton) {
        let ageValue:Int? = Int(ageTextField.text!)
        if (ageValue != nil){
            
            //This track will create an event for this userId that can be viewed
            Leanplum.track("User Logging In!")
            
            
            //Here we set the userAttributes of the user
            var userAttributeDict = [AnyHashable:Any]()
            userAttributeDict["Age"] = ageValue!
            Leanplum.setUserAttributes(userAttributeDict)
        }
    }

}
