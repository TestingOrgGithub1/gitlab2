package com.commit451.gitlab.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import coil.api.load
import coil.transform.CircleCropTransformation
import com.commit451.addendum.themeAttrColor
import com.commit451.alakazam.fadeOut
import com.commit451.easel.Easel
import com.commit451.gitlab.App
import com.commit451.gitlab.BuildConfig
import com.commit451.gitlab.R
import com.commit451.gitlab.activity.*
import com.commit451.gitlab.adapter.AccountAdapter
import com.commit451.gitlab.data.Prefs
import com.commit451.gitlab.event.CloseDrawerEvent
import com.commit451.gitlab.event.LoginEvent
import com.commit451.gitlab.event.ReloadDataEvent
import com.commit451.gitlab.model.Account
import com.commit451.gitlab.model.api.User
import com.commit451.gitlab.navigation.Navigator
import com.commit451.gitlab.rx.CustomResponseSingleObserver
import com.commit451.gitlab.util.ImageUtil
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.util.*

/**
 * Our very own navigation view
 */
class LabCoatNavigationView : NavigationView {

    @BindView(R.id.profile_image)
    lateinit var imageProfile: ImageView
    @BindView(R.id.profile_user)
    lateinit var textUserName: TextView
    @BindView(R.id.profile_email)
    lateinit var textEmail: TextView
    @BindView(R.id.arrow)
    lateinit var iconArrow: View
    @BindView(R.id.button_debug)
    lateinit var buttonDebug: View

    private lateinit var listAccounts: RecyclerView
    private lateinit var adapterAccounts: AccountAdapter
    var user: User? = null

    private val onNavigationItemSelectedListener = OnNavigationItemSelectedListener { menuItem ->
        when (menuItem.itemId) {
            R.id.nav_projects -> {
                if (context !is ProjectsActivity) {
                    Navigator.navigateToProjects(context as Activity)
                    (context as Activity).finish()
                    (context as Activity).overridePendingTransition(R.anim.fade_in, R.anim.do_nothing)
                }
                App.bus().post(CloseDrawerEvent())
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_groups -> {
                if (context !is GroupsActivity) {
                    Navigator.navigateToGroups(context as Activity)
                    (context as Activity).finish()
                    (context as Activity).overridePendingTransition(R.anim.fade_in, R.anim.do_nothing)
                }
                App.bus().post(CloseDrawerEvent())
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_activity -> {
                if (context !is ActivityActivity) {
                    Navigator.navigateToActivity(context as Activity)
                    (context as Activity).finish()
                    (context as Activity).overridePendingTransition(R.anim.fade_in, R.anim.do_nothing)
                }
                App.bus().post(CloseDrawerEvent())
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_todos -> {
                if (context !is TodosActivity) {
                    Navigator.navigateToTodos(context as Activity)
                    (context as Activity).finish()
                    (context as Activity).overridePendingTransition(R.anim.fade_in, R.anim.do_nothing)
                }
                App.bus().post(CloseDrawerEvent())
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_about -> {
                App.bus().post(CloseDrawerEvent())
                Navigator.navigateToAbout(context as Activity)
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_settings -> {
                App.bus().post(CloseDrawerEvent())
                Navigator.navigateToSettings(context as Activity)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private val accountsAdapterListener = object : AccountAdapter.Listener {
        override fun onAccountClicked(account: Account) {
            switchToAccount(account)
        }

        override fun onAddAccountClicked() {
            Navigator.navigateToLogin(context as Activity, true)
            toggleAccounts()
            App.bus().post(CloseDrawerEvent())
        }

        override fun onAccountLogoutClicked(account: Account) {
            Prefs.removeAccount(account)
            val accounts = Prefs.getAccounts()

            if (accounts.isEmpty()) {
                Navigator.navigateToLogin(context as Activity)
                (context as Activity).finish()
            } else {
                if (account == App.get().getAccount()) {
                    switchToAccount(accounts[0])
                }
            }
        }
    }

    @OnClick(R.id.profile_image)
    fun onUserImageClick(imageView: ImageView) {
        user?.let {
            Navigator.navigateToUser(context as Activity, imageView, it)
        }
    }

    @OnClick(R.id.button_debug)
    fun onDebugClicked() {
        context.startActivity(DebugActivity.newIntent(context))
    }

    @OnClick(R.id.drawer_header)
    fun onHeaderClick() {
        toggleAccounts()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    fun init() {
        App.bus().register(this)

        setNavigationItemSelectedListener(onNavigationItemSelectedListener)
        inflateMenu(R.menu.navigation)
        val header = inflateHeaderView(R.layout.header_nav_drawer)
        ButterKnife.bind(this, header)

        if (BuildConfig.DEBUG) {
            buttonDebug.visibility = View.VISIBLE
        }
        listAccounts = RecyclerView(context)
        listAccounts.layoutManager = LinearLayoutManager(context)
        listAccounts.setBackgroundColor(context.themeAttrColor(android.R.attr.windowBackground))
        addView(listAccounts)
        val params = listAccounts.layoutParams as LayoutParams
        params.setMargins(0, resources.getDimensionPixelSize(R.dimen.account_header_height), 0, 0)
        listAccounts.visibility = View.GONE
        adapterAccounts = AccountAdapter(context, accountsAdapterListener)
        listAccounts.adapter = adapterAccounts
        setSelectedNavigationItem()
        setAccounts()
        loadCurrentUser()
    }

    @SuppressLint("RestrictedApi")
    override fun onDetachedFromWindow() {
        App.bus().unregister(this)
        super.onDetachedFromWindow()
    }

    private fun setSelectedNavigationItem() {
        when (context) {
            is ProjectsActivity -> {
                setCheckedItem(R.id.nav_projects)
            }
            is GroupsActivity -> {
                setCheckedItem(R.id.nav_groups)
            }
            is ActivityActivity -> {
                setCheckedItem(R.id.nav_activity)
            }
            is TodosActivity -> {
                setCheckedItem(R.id.nav_todos)
            }
            else -> {
                throw IllegalStateException("You need to defined a menu item for this activity")
            }
        }
    }

    private fun setAccounts() {
        val accounts = Prefs.getAccounts()
        Timber.d("Got %s accounts", accounts.size)
        adapterAccounts.setAccounts(accounts)
    }

    private fun loadCurrentUser() {
        App.get().gitLab.getThisUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : CustomResponseSingleObserver<User>() {

                    override fun error(e: Throwable) {
                        Timber.e(e)
                    }

                    override fun responseNonNullSuccess(userFull: User) {
                        //Store the newly retrieved user to the account so that it stays up to date
                        // in local storage
                        bindUser(userFull)
                    }
                })
    }

    fun bindUser(user: User) {
        if (user.username != null) {
            textUserName.text = user.username
        }
        if (user.email != null) {
            textEmail.text = user.email
        }
        val url = ImageUtil.getAvatarUrl(user, resources.getDimensionPixelSize(R.dimen.larger_image_size))
        imageProfile.load(url) {
            transformations(CircleCropTransformation())
        }
    }

    /**
     * Toggle the visibility of accounts. Meaning hide it if it is showing, show it if it is hidden
     */
    fun toggleAccounts() {
        if (listAccounts.visibility == View.GONE) {
            listAccounts.visibility = View.VISIBLE
            listAccounts.alpha = 0.0f
            listAccounts.animate().alpha(1.0f)
            iconArrow.animate().rotation(180.0f)
        } else {
            listAccounts.fadeOut()
            iconArrow.animate().rotation(0.0f)
        }
    }

    fun switchToAccount(account: Account) {
        Timber.d("Switching to account: %s", account)
        account.lastUsed = Date()
        App.get().setAccount(account)
        Prefs.updateAccount(account)
        toggleAccounts()
        App.bus().post(ReloadDataEvent())
        App.bus().post(CloseDrawerEvent())
        // Trigger a reload in the adapter so that we will place the accounts
        // in the correct order from most recently used
        adapterAccounts.notifyDataSetChanged()
        loadCurrentUser()
    }

    @Subscribe
    fun onUserLoggedIn(event: LoginEvent) {
        adapterAccounts.addAccount(event.account)
        adapterAccounts.notifyDataSetChanged()
        loadCurrentUser()
    }
}
