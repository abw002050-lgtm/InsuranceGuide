package com.insuranceguide.app.data.database

data class LawItem(val id:Long,val title:String,val path:String)
class LawRepository(context: android.content.Context){
    private val db = InsuranceDatabase.get(context.applicationContext).readableDatabase()
    fun laws():List<LawItem>{ val r=mutableListOf<LawItem>(); db.rawQuery("SELECT _id,LAW_TITLE,LAW_DESC FROM LAWS ORDER BY _id",null).use{c->while(c.moveToNext()) r+=LawItem(c.getLong(0),c.getString(1)?:"",c.getString(2)?:"")}; return r }
    fun procedures():List<LawItem>{ val r=mutableListOf<LawItem>(); db.rawQuery("SELECT _id,LAWP_TITLE,LAWP_DESC FROM LAWP ORDER BY _id",null).use{c->while(c.moveToNext()) r+=LawItem(c.getLong(0),c.getString(1)?:"",c.getString(2)?:"")}; return r }
}
