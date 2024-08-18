package com.example.todoapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class RecyclerAdapter(private val todoList: ArrayList<Todo>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolderItem>() {

    // リストに表示するアイテムの表示内容
    inner class ViewHolderItem(v: View) : RecyclerView.ViewHolder(v) {
        val titleHolder : TextView = v.findViewById(R.id.title)
        val detailHolder : TextView = v.findViewById(R.id.person)
    }

    // リストに表示するアイテムを生成
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.one_layout, parent, false)

        return ViewHolderItem(view)
    }

    // position番目のデータを表示
    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
        val currentItem = todoList[position]
        holder.titleHolder.text = currentItem.title
        holder.detailHolder.text = currentItem.person
    }

    // リストサイズを取得する用のメソッド
    override fun getItemCount(): Int {
        return todoList.size
    }
}