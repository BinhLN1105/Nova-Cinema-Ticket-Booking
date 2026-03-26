package com.cinema.ticket_booking.ui.booking;

import androidx.lifecycle.*;
import com.cinema.ticket_booking.data.model.response.ComboResponse;
import com.cinema.ticket_booking.data.repository.ComboRepository;
import com.cinema.ticket_booking.util.Resource;
import java.util.*;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SelectComboViewModel extends ViewModel {
    private final ComboRepository comboRepo;
    private final MutableLiveData<Resource<List<ComboResponse>>> combos = new MutableLiveData<>();
    private final Map<String, Integer> selectedCombos = new LinkedHashMap<>();

    // Static fields to pass data to ConfirmBookingFragment
    // (Follows the pattern established by SelectSeatViewModel)
    public static Map<String, Integer> pendingCombos = new LinkedHashMap<>();

    @Inject
    public SelectComboViewModel(ComboRepository comboRepo) {
        this.comboRepo = comboRepo;
        loadCombos();
    }

    public LiveData<Resource<List<ComboResponse>>> getCombos() {
        return combos;
    }

    public Map<String, Integer> getSelectedCombos() {
        return selectedCombos;
    }

    private void loadCombos() {
        comboRepo.getCombos().observeForever(combos::setValue);
    }

    public void addCombo(String comboId) {
        selectedCombos.merge(comboId, 1, Integer::sum);
    }

    public void removeCombo(String comboId) {
        selectedCombos.computeIfPresent(comboId, (k, v) -> v > 1 ? v - 1 : null);
        if (selectedCombos.containsKey(comboId) && selectedCombos.get(comboId) == null) {
            selectedCombos.remove(comboId);
        }
    }

    public double calculateTotalCombos(List<ComboResponse> allCombos) {
        double total = 0;
        for (ComboResponse combo : allCombos) {
            Integer qty = selectedCombos.get(combo.getId());
            if (qty != null && qty > 0) {
                total += combo.getPrice() * qty;
            }
        }
        return total;
    }
}
