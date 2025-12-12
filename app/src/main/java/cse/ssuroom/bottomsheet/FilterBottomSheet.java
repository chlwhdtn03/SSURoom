package cse.ssuroom.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.RangeSlider;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import cse.ssuroom.R;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IS_LEASE_TRANSFER = "is_lease_transfer";
    private static final String ARG_SHOW_PRICE_FILTER = "show_price_filter";

    private boolean isLeaseTransfer;
    private boolean showPriceFilter;

    // Views
    private TextView priceTitleText;
    private TextView minPriceText;
    private TextView maxPriceText;
    private TextView minScoreText;
    private TextView maxScoreText;
    private TextView minDurationText;
    private TextView maxDurationText;
    private RangeSlider scoreSlider;
    private RangeSlider priceSlider;
    private RangeSlider durationSlider;
    private Button resetButton;
    private Button applyButton;
    private ImageView closeButton;

    private ViewGroup priceContainer; // 가격 레이아웃 컨테이너

    private FilterListener filterListener;

    // 필터 값
    private float minScore = 0;
    private float maxScore = 100;
    private float minPrice;
    private float maxPrice;
    private float minDuration = 1;
    private float maxDuration = 52;

    public interface FilterListener {
        void onFilterApplied(float minScore, float maxScore, float minPrice, float maxPrice, float minDuration, float maxDuration);
    }

    public static FilterBottomSheet newInstance(boolean isLeaseTransfer) {
        return newInstance(isLeaseTransfer, true); // 기본값: 가격 필터 보이기
    }

    public static FilterBottomSheet newInstance(boolean isLeaseTransfer, boolean showPriceFilter) {
        FilterBottomSheet fragment = new FilterBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_LEASE_TRANSFER, isLeaseTransfer);
        args.putBoolean(ARG_SHOW_PRICE_FILTER, showPriceFilter);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isLeaseTransfer = getArguments().getBoolean(ARG_IS_LEASE_TRANSFER, false);
            showPriceFilter = getArguments().getBoolean(ARG_SHOW_PRICE_FILTER, true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        // 가격 필터 표시 여부 설정
        if (!showPriceFilter) {
            hidePriceFilter();
        } else {
            setupPriceFilter();
        }

        setupListeners();
    }

    private void initViews(View view) {
        closeButton = view.findViewById(R.id.iv_close);
        priceTitleText = view.findViewById(R.id.price_title);
        minPriceText = view.findViewById(R.id.min_price_text);
        maxPriceText = view.findViewById(R.id.max_price_text);
        minScoreText = view.findViewById(R.id.min_score_text);
        maxScoreText = view.findViewById(R.id.max_score_text);
        minDurationText = view.findViewById(R.id.min_duration_text);
        maxDurationText = view.findViewById(R.id.max_duration_text);
        scoreSlider = view.findViewById(R.id.score_slider);
        priceSlider = view.findViewById(R.id.price_slider);
        durationSlider = view.findViewById(R.id.duration_slider);
        resetButton = view.findViewById(R.id.reset_button);
        applyButton = view.findViewById(R.id.apply_button);

        // 가격 컨테이너 찾기 (가격 텍스트의 부모의 부모)
        priceContainer = (ViewGroup) minPriceText.getParent().getParent();
    }

    private void hidePriceFilter() {
        // 가격 관련 뷰들 숨기기
        priceTitleText.setVisibility(View.GONE);
        priceContainer.setVisibility(View.GONE);
        priceSlider.setVisibility(View.GONE);

        // 가격 기본값 설정 (필터 적용 시 영향 없도록)
        minPrice = 0;
        maxPrice = Float.MAX_VALUE;
    }

    private void setupPriceFilter() {
        if (isLeaseTransfer) {
            // 계약양도
            priceTitleText.setText("월세");
            minPrice = 10;  // 10만원
            maxPrice = 200; // 200만원
            priceSlider.setValueFrom(10);
            priceSlider.setValueTo(200);
            priceSlider.setStepSize(5);
            priceSlider.setValues(10f, 200f);
        } else {
            // 단기임대
            priceTitleText.setText("주당 가격");
            minPrice = 50000;
            maxPrice = 500000;
            priceSlider.setValueFrom(50000);
            priceSlider.setValueTo(500000);
            priceSlider.setStepSize(10000);
            priceSlider.setValues(50000f, 500000f);
        }
        updatePriceText(minPrice, maxPrice);
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        // 슈방 점수 슬라이더
        scoreSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minScore = values.get(0);
            maxScore = values.get(1);
            updateScoreText(minScore, maxScore);
        });

        // 가격 슬라이더
        if (showPriceFilter) {
            priceSlider.addOnChangeListener((slider, value, fromUser) -> {
                List<Float> values = slider.getValues();
                minPrice = values.get(0);
                maxPrice = values.get(1);
                updatePriceText(minPrice, maxPrice);
            });
        }

        // 임대 기간 슬라이더
        durationSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minDuration = values.get(0);
            maxDuration = values.get(1);
            updateDurationText(minDuration, maxDuration);
        });

        // 초기화 버튼
        resetButton.setOnClickListener(v -> resetFilters());

        // 적용 버튼
        applyButton.setOnClickListener(v -> {
            if (filterListener != null) {
                filterListener.onFilterApplied(minScore, maxScore, minPrice, maxPrice, minDuration, maxDuration);
            }
            dismiss();
        });
    }

    private void updateScoreText(float min, float max) {
        minScoreText.setText(String.format("%.0f점", min));
        maxScoreText.setText(String.format("%.0f점", max));
    }

    private void updatePriceText(float min, float max) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);

        if (isLeaseTransfer) {
            // 계약양도: 만원 단위로 표시
            minPriceText.setText(formatter.format((int) min) + "만원");
            maxPriceText.setText(formatter.format((int) max) + "만원");
        } else {
            // 단기임대: 원 단위로 표시
            minPriceText.setText(formatter.format((int) min) + "원");
            maxPriceText.setText(formatter.format((int) max) + "원");
        }
    }

    private void updateDurationText(float min, float max) {
        if (max >= 52) {
            minDurationText.setText(String.format("%.0f주", min));
            maxDurationText.setText("1년 이상");
        } else {
            minDurationText.setText(String.format("%.0f주", min));
            maxDurationText.setText(String.format("%.0f주", max));
        }
    }

    private void resetFilters() {
        // 슈방 점수 초기화
        minScore = 0;
        maxScore = 100;
        scoreSlider.setValues(0f, 100f);
        updateScoreText(minScore, maxScore);

        // 가격 초기화 (가격 필터가 보일 때만)
        if (showPriceFilter) {
            if (isLeaseTransfer) {
                minPrice = 10;  // 10만원
                maxPrice = 200; // 200만원
                priceSlider.setValues(10f, 200f);
            } else {
                minPrice = 50000;
                maxPrice = 500000;
                priceSlider.setValues(50000f, 500000f);
            }
            updatePriceText(minPrice, maxPrice);
        }

        // 임대 기간 초기화
        minDuration = 1;
        maxDuration = 52;
        durationSlider.setValues(1f, 52f);
        updateDurationText(minDuration, maxDuration);
    }
}