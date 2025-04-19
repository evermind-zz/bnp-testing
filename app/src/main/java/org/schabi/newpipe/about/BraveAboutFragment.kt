package org.schabi.newpipe.about

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.schabi.newpipe.databinding.FragmentAboutBinding

class BoolWrapper(isAlreadyAdded: Boolean) {
    var value: Boolean = isAlreadyAdded
}

open class BraveAboutFragment : Fragment() {

    protected fun braveSetupViews(
        grp: ViewGroup,
        fragmentAboutBinding: FragmentAboutBinding
    ) {
        braveSetAllNoneBravePipeViewsGone(grp, fragmentAboutBinding)
    }

    private fun braveSetAllNoneBravePipeViewsGone(
        grp: ViewGroup,
        fragmentAboutBinding: FragmentAboutBinding
    ) {
        val doSetRemainingViewsGone = BoolWrapper(false)
        braveSetAllNoneBravePipeViewsGoneRecursive(
            doSetRemainingViewsGone,
            grp,
            fragmentAboutBinding
        )
    }

    private fun braveSetAllNoneBravePipeViewsGoneRecursive(
        doSetRemainingViewsGone: BoolWrapper,
        grp: ViewGroup,
        fragmentAboutBinding: FragmentAboutBinding
    ) {
        for (x in 0 until grp.childCount) {
            val child = grp.getChildAt(x)
            if (child is ViewGroup) {
                if (child == fragmentAboutBinding.braveAbout.root) { // all later view's are not relevant for BravePipe
                    doSetRemainingViewsGone.value = true
                } else {
                    braveSetAllNoneBravePipeViewsGoneRecursive(
                        doSetRemainingViewsGone,
                        child as ViewGroup,
                        fragmentAboutBinding
                    )
                }
            } else {
                if (doSetRemainingViewsGone.value) {
                    child.visibility = View.GONE
                }
            }
        }
    }

    protected fun braveAddSoftwareComponents(softwareComponents: ArrayList<SoftwareComponent>) {
        if (areSoftwareComponentsAlreadyAdded) {
            return; // run only once
        }
        areSoftwareComponentsAlreadyAdded = true
        softwareComponents.add(
            SoftwareComponent(
                "EventBus", "2012 - 2016", "Markus Junginger",
                "https://greenrobot.org/eventbus", StandardLicenses.APACHE2
            )
        )
        softwareComponents.add(
            SoftwareComponent(
                "OsExt", "2024", "evermind-zz",
                "https://github.com/evermind-zz/OsExt", StandardLicenses.GPL3
            )
        )
    }

    companion object {
        private var areSoftwareComponentsAlreadyAdded: Boolean = false
    }
}
