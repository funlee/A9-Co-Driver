package dev.funlee.a9.core;

import java.nio.file.*;
import java.util.*;

final class TaskRouteChecks {
    static int run(Path assets)throws Exception {
        int checks=0;
        for(String module:List.of("cup","daily","special"))try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_validateRoute")){l.push(Route.parse(l.toString(1),l.toString(2)).valid());return 1;}
            if(name.equals("_routeDisplay")){l.push("已选择地图");return 1;}return 0;
        },List.of("_validateRoute","_routeDisplay"))) {
            lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
            lua.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
            lua.load("setup","data={};run={data={}};utils={};ROUTES={chosen='0,21'};LOCAL_ROOT='/tmp/test';UIconfig={gamePkg='test'};multiple1={};multiple2={};dsCup={};cup=cup or {};spCup=spCup or {};dailyCup=dailyCup or {};localInit();"+
                "starts=0;utils.startConfiguredRace=function()starts=starts+1;return 0 end;function _dailyTaskStatus()return '' end;"+
                "workflow="+(module.equals("cup")?"cup":module.equals("daily")?"dailyCup":"spCup")+";"+
                "run.data={['路线来源']='0',['已配置地图']='chosen',['杯赛选路']='1',['特殊赛事选路']='2'};"+
                "assert(workflow.operation[5][2]()==-1);assert(starts==1 and data['线路']=='0,21' and data['多人地图']=='已选择地图');"+
                "ROUTES.chosen='10,22';utils.prepareTaskRoute();assert(data['线路']=='10,22');"+
                "run.data['路线来源']='1';run.data['跑法代码']='5,D:100';utils.prepareTaskRoute();assert(data['线路']=='5,D:100');"+
                "for _,code in ipairs({'','bad'})do run.data['跑法代码']=code;assert(not pcall(workflow.operation[5][2]));assert(starts==1) end;"+
                "run.data['路线来源']='0';run.data['已配置地图']='missing';assert(not pcall(workflow.operation[5][2]));assert(starts==1);"+
                "run.data['路线来源']='1';run.data['跑法代码']='0,21';drivers=0;utils.newAutoDriver=function()drivers=drivers+1;assert(data['线路']=='0,21') end;"+
                "function keepCapture()end;function cmpColorEx(pattern)return string.find(pattern,'680|50',1,true) and 0 or 1 end;"+
                "function tap()error('Unexpected tap')end;function swipe()error('Retired direction choice')end;"+
                "function test()workflow.operation[7][2]();assert(drivers==1)end");
            lua.start("test");lua.step(0);checks+=7;
        }
        for(String module:List.of("cup","daily"))try(LuaRuntime lua=new LuaRuntime((name,l)->{
            if(name.equals("_validateRoute")){l.push(Route.parse(l.toString(1),l.toString(2)).valid());return 1;}
            if(name.equals("_routeDisplay")){l.push("自动识别地图");return 1;}return 0;
        },List.of("_validateRoute","_routeDisplay"))) {
            lua.load("bootstrap",Files.readString(assets.resolve("lua/bootstrap.lua")));
            lua.load(module,Files.readString(assets.resolve("lua/"+module+".lua")));
            lua.load("auto setup","data={['项目']='"+(module.equals("cup")?"杯赛":"每日战利品")+"'};run={data={['路线来源']='2',['跑法代码']='99,22',['已配置地图']='other'}};utils={};ROUTES={chosen='0,21'};LOCAL_ROOT='/tmp/test';UIconfig={gamePkg='test'};multiple1={};multiple2={};dsCup={};cup=cup or {};spCup={};dailyCup=dailyCup or {};localInit();"+
                "workflow="+(module.equals("cup")?"cup":"dailyCup")+";function print()end;function keepCapture()end;function releaseCapture()end;function _dailyTaskStatus()return '' end;"+
                "local stage=0;function cmpColorEx(pattern)if string.find(pattern,'664|436',1,true) then return stage==0 and 1 or 0 end;return stage==2 and 1 or 0 end;"+
                "utils.localOCR=function()return 'chosen' end;reads=0;function _soloEventUi()reads=reads+1;return reads<3 and {scene='loading',route='chosen'} or {scene='race'} end;"+
                "function test()assert(utils.taskRouteAuto());utils.prepareTaskRoute(true);assert(data['线路']=='');workflow.readMap();assert(data['线路']=='0,21');"+
                "utils.prepareTaskRoute();assert(data['线路']=='0,21');"+
                "stage=2;reads=9;utils.prepareTaskRoute(true);workflow.readMap();assert(data['线路']=='','Previous race route leaked into failed recognition');"+
                "stage=0;reads=0;ROUTES.chosen='10,31';workflow.readMap();assert(data['线路']=='10,31');"+
                "local read=workflow.readMap;local calls,manual=0,0;workflow.readMap=function(readRoute)if readRoute==false then manual=manual+1 else calls=calls+1 end;read(readRoute);stage=2 end;utils.startConfiguredRace=function()return 1 end;stage=0;reads=0;workflow.operation[5][2]();assert(calls==1 and manual==0 and data['线路']=='10,31');"+
                "run.data['路线来源']='1';run.data['跑法代码']='5,22';workflow.operation[5][2]();assert(calls==1 and manual==(workflow==dailyCup and 1 or 0) and not utils.taskRouteAuto() and data['线路']=='5,22');end");
            lua.start("test");boolean alive=true;for(int i=0;i<60&&alive;i++)alive=lua.step(i*1000L);
            if(alive)throw new AssertionError("Automatic map test did not finish");checks+=6;
        }
        return checks;
    }
}
