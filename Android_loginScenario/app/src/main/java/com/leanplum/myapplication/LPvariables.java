package com.leanplum.myapplication;

import com.leanplum.Var;
import com.leanplum.annotations.Variable;

/**
 * Created by fede on 10/12/16.
 */

public class LPvariables {

    @Variable
    public static String welcomeMessage = "Welcome to Leanplum";

    @Variable
    public static int LPint = 12;


    static Var<String> snoopy = Var.defineAsset("Snoopy", "snoopy.png");

}
