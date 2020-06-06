package Adapter

import Model.Story
import Model.User
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.instagramcloneapp.AddStoryActivity
import com.example.instagramcloneapp.MainActivity
import com.example.instagramcloneapp.R
import com.example.instagramcloneapp.StoryActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.fragment_profile.view.*

class StoryAdapter (private val mContext: Context, private val mstory: List<Story>) :
RecyclerView.Adapter<StoryAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        return if (viewType == 0)
        {
            val view = LayoutInflater.from(mContext).inflate(R.layout.add_story_item, parent, false)
            ViewHolder(view)
        }
        else
        {
            val view = LayoutInflater.from(mContext).inflate(R.layout.story_item, parent, false)
            ViewHolder(view)
        }

    }

    override fun getItemCount(): Int
    {
        return mstory.size

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val story = mstory[position]

        userInfo(holder, story.getUserId(), position)

        if (holder.adapterPosition !== 0)
        {
            seenStory(holder, story.getUserId())
        }
        if (holder.adapterPosition === 0)
        {
            myStories(holder.addStory_text!!, holder.story_plus_btn!!, false)
        }

        holder.itemView.setOnClickListener {
            if (holder.adapterPosition === 0)
            {
                myStories(holder.addStory_text!!, holder.story_plus_btn!!, true)
            }
            else
            {
                val intent = Intent(mContext, StoryActivity::class.java)
                intent.putExtra("userId", story.getUserId())
                mContext.startActivity(intent)
            }
        }
    }



    inner class ViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        //StoryItem
        var story_image_seen: CircleImageView? = null
        var story_image: CircleImageView? = null
        var story_username: TextView? = null

        //AddStoryItem
        var story_plus_btn: ImageView? = null
        var addStory_text: TextView? = null

        init {
            //StoryItem
            story_image_seen = itemView.findViewById(R.id.story_image_seen)
            story_image = itemView.findViewById(R.id.story_image)
            story_username = itemView.findViewById(R.id.story_username)


            //AddStoryItem
            story_plus_btn = itemView.findViewById(R.id.story_add)
            addStory_text = itemView.findViewById(R.id.add_story_text)

        }
    }


    override fun getItemViewType(position: Int): Int {
        if (position == 0)
        {
            return 0
        }
        return 1
    }


    private fun userInfo(viewHolder: ViewHolder, userId: String, position: Int)
    {
        val usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                if(p0.exists())
                {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(viewHolder.story_image)

                    if (position!=0)
                    {
                        Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(viewHolder.story_image_seen)
                        viewHolder.story_username!!.text = user.getUsername()
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError)
            {

            }
        })
    }


    private fun myStories(textView: TextView, imageView: ImageView, click: Boolean)
    {
        val storyRef = FirebaseDatabase.getInstance().reference
            .child("Story").child(FirebaseAuth.getInstance().currentUser!!.uid)

        storyRef.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
               var counter = 0

                val timeCurrent = System.currentTimeMillis()

                for (snapshot in p0.children)
                {
                    val story = snapshot.getValue(Story::class.java)

                    if (timeCurrent>story!!.getTimeStart() && timeCurrent<story!!.getTimeEnd())
                    {
                        counter++
                    }
                }
                if (click)
                {
                    if (counter > 0) {
                        val alertDialog = AlertDialog.Builder(mContext).create()

                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "View Story")
                        { dialogInterface, which ->

                            val intent = Intent(mContext, StoryActivity::class.java)
                            intent.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
                            mContext.startActivity(intent)
                            dialogInterface.dismiss()
                        }

                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Add Story")
                        { dialogInterface, which ->

                            val intent = Intent(mContext, AddStoryActivity::class.java)
                            intent.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
                            mContext.startActivity(intent)
                            dialogInterface.dismiss()
                        }

                        alertDialog.show()
                    }
                    else
                    {
                        val intent = Intent(mContext, AddStoryActivity::class.java)
                        intent.putExtra("userid", FirebaseAuth.getInstance().currentUser!!.uid)
                        mContext.startActivity(intent)
                    }

                }
                else
                {
                    if (counter>0)
                    {
                        textView.text = "My Story"
                        imageView.visibility = View.GONE
                    }
                    else
                    {
                        textView.text = "Add Story"
                        imageView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError)
            {

            }


        })
    }




    private fun seenStory(viewHolder: ViewHolder, userId: String)
    {
        val storyRef = FirebaseDatabase.getInstance().reference
            .child("Story").child(userId)

        storyRef.addValueEventListener(object : ValueEventListener
        {
            override fun onDataChange(p0: DataSnapshot)
            {
                var i = 0
                for (snapshot in p0.children)
                {
                    if (!snapshot.child("views")
                            .child(FirebaseAuth.getInstance().currentUser!!.uid).exists()
                        && System.currentTimeMillis() < snapshot.getValue(Story::class.java)!!.getTimeEnd())
                    {
                        i++
                    }
                }
                if (i>0)
                {
                    viewHolder.story_image!!.visibility = View.VISIBLE
                    viewHolder.story_image_seen!!.visibility = View.GONE
                }
                else
                {
                    viewHolder.story_image!!.visibility = View.GONE
                    viewHolder.story_image_seen!!.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(p0: DatabaseError)
            {

            }
        })
    }


}