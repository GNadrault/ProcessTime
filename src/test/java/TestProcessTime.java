import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.swing.text.ZoneView;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProcessTime {


    // a message have 2 dates : the reception date (startDate) and the archived date (endDate)
    // the process time is only calculated during the period of activity define by startActiveHour(0-23) , startActiveMinute (0-59) ,endActiveHour (1-24)  , endActiveMinute (0-59)
    // LIMITATION : less than 48h between the startDate and the endDate
    // return in seconds the process time

    public long calculateProcessTime (@NotNull Instant startDate , @NotNull Instant endDate , int startActiveHour , int startActiveMinute , int endActiveHour , int endActiveMinute ){
        long processTime = 0;

        if (startDate.isBefore(endDate)) { // Check if startDate is Before endDate else processTime = 0
            ZonedDateTime startZoneDateTime = startDate.atZone(ZoneOffset.UTC);
            ZonedDateTime endZoneDateTime = endDate.atZone(ZoneOffset.UTC);
            LocalTime startActiveTime = LocalTime.of(startActiveHour, startActiveMinute, 0);
            LocalTime endActiveTime = LocalTime.of(endActiveHour == 24 ? 0 : endActiveHour, endActiveMinute, 0);

            // If startDate is before startActiveDate then startDate = startActiveDate
            if (startZoneDateTime.getHour() < startActiveHour
                    || (startZoneDateTime.getHour() == startActiveHour && startZoneDateTime.getMinute() < startActiveMinute)) {
                startZoneDateTime = startZoneDateTime.with(startActiveTime);
            }

            // If endDate is after endActiveDate then endDate = endActiveDate
            if (endZoneDateTime.getHour() > endActiveHour
                    || (endZoneDateTime.getHour() == endActiveHour && endZoneDateTime.getMinute() > endActiveMinute)) {
                endZoneDateTime = endZoneDateTime.with(endActiveTime);
            }

            // Check if same day or other day
            int nbDayDiff = (int) ChronoUnit.DAYS.between(startZoneDateTime, endZoneDateTime);
            if (nbDayDiff > 0) { // If other day
                nbDayDiff = nbDayDiff > 1 ? 1 : nbDayDiff; // Set the maximum of day difference = 1, limitation of 48h
                ZonedDateTime currentZoneDateTime = startZoneDateTime;
                for (int i=0; i<=nbDayDiff; i++) {
                    processTime += checkBoundaryAndCalculatePeriod(currentZoneDateTime, endZoneDateTime, endActiveTime);
                    currentZoneDateTime = currentZoneDateTime.plusDays(1).with(startActiveTime);
                }
            } else { // If same day
                processTime += Duration.between(startZoneDateTime, endZoneDateTime).getSeconds();
            }
        }
        return processTime;
    }

    /**
     * Calculate the period between the currentZoneDateTime and the endZoneDateTime if their are in the same day, otherwise with the endActiveTime
     * @param currentZoneDateTime
     * @param endZoneDateTime
     * @param endActiveTime
     * @return
     */
    private long checkBoundaryAndCalculatePeriod(ZonedDateTime currentZoneDateTime, ZonedDateTime endZoneDateTime, LocalTime endActiveTime) {
        ZonedDateTime endCurrentZoneDateTime;
        if (ChronoUnit.DAYS.between(currentZoneDateTime, endZoneDateTime) == 0) { // Same day => boundary is the endDate
            endCurrentZoneDateTime = endZoneDateTime;
        } else {
            endCurrentZoneDateTime = currentZoneDateTime.with(endActiveTime);
            if (endActiveTime.getHour() == 0 && endActiveTime.getMinute() == 0) { // Other day => boundary is the endActiveDate
                endCurrentZoneDateTime = endCurrentZoneDateTime.plusDays(1);
            }
        }
        return Duration.between(currentZoneDateTime, endCurrentZoneDateTime).getSeconds();
    }


    @Test
    public void testSameDayNoInActive() {

        int startActiveHour  = 0;
        int startActiveMinute = 00;
        int endActiveHour = 24;
        int endActiveMinute = 0;


        Instant start = Instant.parse("2021-02-10T23:59:59Z");
        Instant end = Instant.parse("2021-02-11T00:00:01Z");


        long processTime = calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(2);
    }


    @Test
    public void testSameDayStartAndEndActive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T16:25:01Z");
        Instant end = Instant.parse("2021-02-10T17:25:01Z");


        long processTime = calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(3600);
    }

    @Test
    public void testSameDayStartAndEndActive2() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 24;
        int endActiveMinute = 00;


        Instant start = Instant.parse("2021-02-10T20:59:59Z");
        Instant end = Instant.parse("2021-02-10T23:59:59Z");


        long processTime = calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(10800);
    }


    @Test
    public void testSameDayStartInactive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T05:25:01Z");
        Instant end = Instant.parse("2021-02-10T17:25:01Z");


        long processTime = calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(35701);
    }

    @Test
    public void testSameDayEndInactive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T17:25:01Z");
        Instant end = Instant.parse("2021-02-10T23:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(11099);
    }

    @Test
    public void testSameDayStartAndEndInactive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T06:25:01Z");
        Instant end = Instant.parse("2021-02-10T23:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);

        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(46800);
    }


    @Test
    public void testOtherDayStartAndEndActive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T16:25:01Z");
        Instant end = Instant.parse("2021-02-11T17:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(50400);
    }

    @Test
    public void testOtherDayStartAndEndActive2() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 24;
        int endActiveMinute = 00;


        Instant start = Instant.parse("2021-02-10T20:59:59Z");
        Instant end = Instant.parse("2021-02-11T23:59:59Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(70200);
    }

    @Test
    public void testOtherDayStartAndEndActive3() {

        int startActiveHour  = 0;
        int startActiveMinute = 0;
        int endActiveHour = 24;
        int endActiveMinute = 00;


        Instant start = Instant.parse("2021-02-10T20:59:59Z");
        Instant end = Instant.parse("2021-02-11T23:59:59Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(97200);
    }


    @Test
    public void testOtherDayStartInactive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T05:25:01Z");
        Instant end = Instant.parse("2021-02-11T17:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(82501);
    }

    @Test
    public void testOtherDayEndInactive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T17:25:01Z");
        Instant end = Instant.parse("2021-02-11T23:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);
        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(57899);
    }

    @Test
    public void testOtherDayStartAndEndInactive() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-02-10T06:25:01Z");
        Instant end = Instant.parse("2021-02-11T23:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);

        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(93600);
    }


    @Test
    public void testVeryLongPeriod() {

        long result =93600;

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;

// 1 day between date
        Instant start = Instant.parse("2021-02-10T06:25:01Z");
        Instant end = Instant.parse("2021-02-11T23:25:01Z");

        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);

        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(result);


        // 10 days between date
        start = Instant.parse("2021-02-01T06:25:01Z");
        end = Instant.parse("2021-02-11T23:25:01Z");

        processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);

        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(result);


    }

    @Test
    public void testBetween2Year() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2020-12-31T06:25:01Z");
        Instant end = Instant.parse("2021-01-01T23:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);

        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(93600);
    }

    @Test
    public void testPublishDateLargerThanDecision() {

        int startActiveHour  = 7;
        int startActiveMinute = 30;
        int endActiveHour = 20;
        int endActiveMinute = 30;


        Instant start = Instant.parse("2021-01-04T10:25:01Z");
        Instant end =   Instant.parse("2021-01-04T09:25:01Z");


        long processTime =calculateProcessTime(start,end,startActiveHour,startActiveMinute,endActiveHour,endActiveMinute);

        System.out.println( "seconde :" + processTime +  " min :" + processTime/60 + " heure : "+ processTime/(60*60) );
        assertThat(processTime).isEqualTo(0);
    }


}
