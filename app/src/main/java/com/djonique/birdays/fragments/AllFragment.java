/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.MainActivity;
import com.djonique.birdays.adapters.AllFragmentAdapter;
import com.djonique.birdays.models.Item;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.models.Separator;
import com.djonique.birdays.repository.BirdaysRepository;
import com.djonique.birdays.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class AllFragment extends Fragment {

    private MainActivity activity;
    private RecyclerView recyclerView;
    private AllFragmentAdapter adapter;
    private DeletingPersonListener deletingPersonListener;
    /**
     * Guards against an older list load or search landing after a newer one. Both paths rewrite
     * the same adapter, so they share a single sequence number.
     */
    private int dataToken;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            deletingPersonListener = (MainActivity) getActivity();
        } catch (ClassCastException ignored) {
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            activity = (MainActivity) getActivity();
            addAllPersonsFromDb();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new AllFragmentAdapter(this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    public void addPerson(Person newPerson, boolean saveToDb) {
        int position = -1;
        Separator separator = null;

        long newPersonDate = newPerson.getDate();
        int newPersonMonth = newPerson.getMonth();
        int newPersonDay = newPerson.getDay();

        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getItem(i).isPerson()) {
                Person person = ((Person) adapter.getItem(i));
                int month = person.getMonth();
                int day = person.getDay();

                if (newPersonMonth < month) {
                    position = i;
                    break;
                } else if (newPersonMonth == month) {
                    if (newPersonDay <= day) {
                        position = i;
                        break;
                    } else {
                        if (adapter.getItemCount() > (i + 1) && adapter.getItem(i + 1).isPerson()) {
                            Person nextPerson = ((Person) adapter.getItem(i + 1));
                            if (newPersonDay <= nextPerson.getDay()) {
                                position = i + 1;
                                break;
                            }
                        } else {
                            position = i + 1;
                            break;
                        }
                    }
                }
            }
        }

        if (newPersonDate != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(newPersonDate);

            separator = getSeparator(newPerson);
        }

        if (position != -1) {
            if (separator != null) {
                adapter.addItem(position - 1, separator);
            }
            adapter.addItem(position, newPerson);
        } else {
            if (separator != null) {
                adapter.addItem(separator);
            }
            adapter.addItem(newPerson);
        }

        if (saveToDb && activity != null) {
            BirdaysRepository.getInstance(activity).addPerson(newPerson, null);
        }
    }

    public void addAllPersonsFromDb() {
        if (activity == null || adapter == null) return;

        final int token = ++dataToken;

        Parcelable listState = null;
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            listState = recyclerView.getLayoutManager().onSaveInstanceState();
        }

        final Parcelable savedState = listState;

        BirdaysRepository.getInstance(activity).getPersons(
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(List<Person> persons) {
                        if (token != dataToken || !isAdded() || adapter == null) return;
                        replaceWithPersons(persons);

                        if (savedState != null && recyclerView != null
                                && recyclerView.getLayoutManager() != null) {
                            recyclerView.getLayoutManager().onRestoreInstanceState(savedState);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Keep the previous list; the next refresh will retry.
                    }
                });
    }

    /**
     * Rebuilds the whole list in one pass.
     *
     * <p>Inserting row by row used to rescan the adapter for every person (O(N²)) and emit one
     * change notification per person and separator. The records are sorted once, grouped by
     * month, and handed to the adapter as a single range insertion.</p>
     */
    private void replaceWithPersons(List<Person> persons) {
        if (adapter == null) return;

        // The XML layout animation runs on the first layout pass only. The records arrive from the
        // database after that pass, so the animation is re-armed for the first batch and the list
        // still falls down into place instead of appearing at once. Refreshes and search results
        // over a non-empty list keep the rows in place.
        boolean firstPopulation = adapter.getItemCount() == 0 && recyclerView != null;

        adapter.removeAllPersons();

        List<Person> sorted = new ArrayList<>(persons);
        Collections.sort(sorted);

        List<Item> newItems = new ArrayList<>(sorted.size() * 2);
        for (Person person : sorted) {
            Separator separator = getSeparator(person);
            if (separator != null) {
                newItems.add(separator);
            }
            newItems.add(person);
        }

        if (firstPopulation) {
            Utils.playListEnterAnimation(recyclerView);
        }
        adapter.addItems(newItems);
    }

    private Separator getSeparator(Person person) {
        Separator separator = null;
        switch (person.getMonth()) {
            case Calendar.JANUARY:
                if (!adapter.containsSeparatorJanuary) {
                    adapter.containsSeparatorJanuary = true;
                    separator = new Separator(Separator.TYPE_JANUARY);
                }
                break;
            case Calendar.FEBRUARY:
                if (!adapter.containsSeparatorFebruary) {
                    adapter.containsSeparatorFebruary = true;
                    separator = new Separator(Separator.TYPE_FEBRUARY);
                }
                break;
            case Calendar.MARCH:
                if (!adapter.containsSeparatorMarch) {
                    adapter.containsSeparatorMarch = true;
                    separator = new Separator(Separator.TYPE_MARCH);
                }
                break;
            case Calendar.APRIL:
                if (!adapter.containsSeparatorApril) {
                    adapter.containsSeparatorApril = true;
                    separator = new Separator(Separator.TYPE_APRIL);
                }
                break;
            case Calendar.MAY:
                if (!adapter.containsSeparatorMay) {
                    adapter.containsSeparatorMay = true;
                    separator = new Separator(Separator.TYPE_MAY);
                }
                break;
            case Calendar.JUNE:
                if (!adapter.containsSeparatorJune) {
                    adapter.containsSeparatorJune = true;
                    separator = new Separator(Separator.TYPE_JUNE);
                }
                break;
            case Calendar.JULY:
                if (!adapter.containsSeparatorJuly) {
                    adapter.containsSeparatorJuly = true;
                    separator = new Separator(Separator.TYPE_JULY);
                }
                break;
            case Calendar.AUGUST:
                if (!adapter.containsSeparatorAugust) {
                    adapter.containsSeparatorAugust = true;
                    separator = new Separator(Separator.TYPE_AUGUST);
                }
                break;
            case Calendar.SEPTEMBER:
                if (!adapter.containsSeparatorSeptember) {
                    adapter.containsSeparatorSeptember = true;
                    separator = new Separator(Separator.TYPE_SEPTEMBER);
                }
                break;
            case Calendar.OCTOBER:
                if (!adapter.containsSeparatorOctober) {
                    adapter.containsSeparatorOctober = true;
                    separator = new Separator(Separator.TYPE_OCTOBER);
                }
                break;
            case Calendar.NOVEMBER:
                if (!adapter.containsSeparatorNovember) {
                    adapter.containsSeparatorNovember = true;
                    separator = new Separator(Separator.TYPE_NOVEMBER);
                }
                break;
            case Calendar.DECEMBER:
                if (!adapter.containsSeparatorDecember) {
                    adapter.containsSeparatorDecember = true;
                    separator = new Separator(Separator.TYPE_DECEMBER);
                }
                break;
        }
        return separator;
    }

    /**
     * Removes a record by its id, e.g. when it was deleted on the detail screen. Uses the adapter
     * removal so the row animates out with the same fade-and-shift effect as a long-press
     * deletion, separators included.
     */
    public void deletePerson(long timeStamp) {
        if (adapter == null) return;

        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getItem(i) instanceof Person person && person.getTimeStamp() == timeStamp) {
                adapter.removePerson(i);
                return;
            }
        }
    }

    public void removePersonDialog(final int location) {
        if (activity == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

        Person person = (Person) adapter.getItem(location);
        final long timeStamp = person.getTimeStamp();

        builder.setMessage(getString(R.string.delete_record_confirm, person.getName()));

        final boolean[] isRemoved = {false};

        builder.setPositiveButton(getString(R.string.ok_button), (dialog, which) -> {
            adapter.removePerson(location);
            isRemoved[0] = true;
            Snackbar snackbar = Snackbar.make(activity.findViewById(R.id.container_main),
                    R.string.record_deleted, Snackbar.LENGTH_SHORT);

            snackbar.setAction(getString(R.string.undo), v -> {
                // Clear the pending-delete flag before the async read: the snackbar dismisses
                // right after the action, and onViewDetachedFromWindow must not delete the row.
                isRemoved[0] = false;
                BirdaysRepository.getInstance(activity).getPerson(timeStamp,
                        new BirdaysRepository.Callback<>() {
                            @Override
                            public void onSuccess(Person person) {
                                if (person == null || !isAdded() || adapter == null) return;
                                addPerson(person, false);
                            }

                            @Override
                            public void onError(Throwable t) {
                                // The row is already gone from the list; the next refresh resyncs.
                            }
                        });
            });

            snackbar.getView().addOnAttachStateChangeListener(
                    new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(@NonNull View v) {
                        }

                        @Override
                        public void onViewDetachedFromWindow(@NonNull View v) {
                            if (isRemoved[0]) {
                                BirdaysRepository.getInstance(activity).removePerson(timeStamp, null);
                                if (deletingPersonListener != null) {
                                    deletingPersonListener.onPersonDeleted(timeStamp);
                                }
                            }
                        }
                    });
            snackbar.show();
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public void findPerson(String name) {
        if (adapter == null || activity == null) return;

        final int token = ++dataToken;

        BirdaysRepository.getInstance(activity).searchPersons(name, false,
                new BirdaysRepository.Callback<>() {
                    @Override
                    public void onSuccess(List<Person> persons) {
                        if (token != dataToken || !isAdded() || adapter == null) return;
                        replaceWithPersons(persons);
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Keep the previous list; the next keystroke retries.
                    }
                });
    }

    public interface DeletingPersonListener {
        void onPersonDeleted(long timeStamp);
    }
}
