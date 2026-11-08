package com.lukk.sky.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lukk.sky.contract.api.OffersApi;
import com.lukk.sky.contract.api.OwnerOffersApi;
import com.lukk.sky.contract.dto.BookingDTO;
import com.lukk.sky.contract.dto.BookingPage;
import com.lukk.sky.contract.dto.MessageDTO;
import com.lukk.sky.contract.dto.MessagePage;
import com.lukk.sky.contract.dto.OfferDTO;
import com.lukk.sky.contract.dto.OfferEditDTO;
import com.lukk.sky.contract.dto.OfferPage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

class ContractGenerationTest {

    @Test
    void offerDtoHotelName_hasPatternAndSizeBounds() {
        Method hotelName = getter(OfferDTO.class, "hotelName");

        assertThat(hotelName.getAnnotation(Pattern.class)).isNotNull();
        Size size = hotelName.getAnnotation(Size.class);
        assertThat(size).isNotNull();
        assertThat(size.min()).isEqualTo(1);
        assertThat(size.max()).isEqualTo(255);
    }

    @Test
    void offerEditDtoHotelName_isNullable_andCarriesNoNotNull() {
        Method hotelName = getter(OfferEditDTO.class, "hotelName");

        assertThat(hotelName.getAnnotation(NotNull.class)).isNull();
    }

    @Test
    void messageDtoReceiverEmail_hasEmailConstraint() {
        Method receiverEmail = getter(MessageDTO.class, "receiverEmail");

        assertThat(receiverEmail.getAnnotation(Email.class)).isNotNull();
    }

    @Test
    void offerDto_carriesJsonIncludeNonNull() {
        JsonInclude jsonInclude = OfferDTO.class.getAnnotation(JsonInclude.class);

        assertThat(jsonInclude).isNotNull();
        assertThat(jsonInclude.value()).isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    void bookingDtoBookedDate_isLocalDate_withoutStringConstraints() {
        Method bookedDate = getter(BookingDTO.class, "bookedDate");

        assertThat(bookedDate.getReturnType()).isEqualTo(LocalDate.class);
        assertThat(bookedDate.getAnnotation(Size.class)).isNull();
        assertThat(bookedDate.getAnnotation(Pattern.class)).isNull();
    }

    @Test
    void offerPageContent_returnsListOfOfferDto() {
        assertThat(genericReturnType(OfferPage.class, "getContent").getTypeName())
            .isEqualTo("java.util.List<com.lukk.sky.contract.dto.OfferDTO>");
    }

    @Test
    void bookingPageContent_returnsListOfBookingDto() {
        assertThat(genericReturnType(BookingPage.class, "getContent").getTypeName())
            .isEqualTo("java.util.List<com.lukk.sky.contract.dto.BookingDTO>");
    }

    @Test
    void messagePageContent_returnsListOfMessageDto() {
        assertThat(genericReturnType(MessagePage.class, "getContent").getTypeName())
            .isEqualTo("java.util.List<com.lukk.sky.contract.dto.MessageDTO>");
    }

    @Test
    void offersApiSearch_bindsTermAsRequestParam_andReturnsOfferPage() {
        Method search = Arrays.stream(OffersApi.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("search"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("OffersApi.search is missing"));

        assertThat(search.getGenericReturnType().getTypeName())
            .isEqualTo("org.springframework.http.ResponseEntity<com.lukk.sky.contract.dto.OfferPage>");

        Parameter term = Arrays.stream(search.getParameters())
            .filter(parameter -> parameter.getType().equals(String.class))
            .findFirst()
            .orElseThrow(() -> new AssertionError("OffersApi.search has no String parameter"));
        assertThat(term.isAnnotationPresent(RequestParam.class)).isTrue();
    }

    @Test
    void ownerOffersApi_exposesEveryOwnerOperation() {
        Set<String> methods = Arrays.stream(OwnerOffersApi.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertThat(methods).contains(
            "edit",
            "uploadPhoto",
            "deletePhoto",
            "uploadGalleryPhoto",
            "deleteGalleryPhoto",
            "setGalleryCover",
            "reorderGalleryPhoto"
        );
    }

    private static Method getter(Class<?> type, String property) {
        String name = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Missing " + type.getSimpleName() + "." + name, ex);
        }
    }

    private static Type genericReturnType(Class<?> type, String methodName) {
        try {
            return type.getMethod(methodName).getGenericReturnType();
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Missing " + type.getSimpleName() + "." + methodName, ex);
        }
    }
}
