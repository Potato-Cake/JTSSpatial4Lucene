package cn.potatocake.lucene.spatial.geohash.utils;

/**
 * 距离计算工具类
 * <p>
 * @author @author Potato-Cake（potato_cake@163.com）
 */
public class DistanceUtils {

    public static final double DEGREES_TO_RADIANS = Math.PI / 180; // 角度转弧度常数
    public static final double RADIANS_TO_DEGREES = 1 / DEGREES_TO_RADIANS; // 弧度转角度常数

    public static final double KM_TO_MILES = 0.621371192; // 公里转英里常数
    public static final double MILES_TO_KM = 1 / KM_TO_MILES;// 英里转公里常数1.609

    public static final double EARTH_MEAN_RADIUS_KM = 6371.0087714;
    public static final double EARTH_EQUATORIAL_RADIUS_KM = 6378.1370;

    public static final double DEG_TO_KM = DEGREES_TO_RADIANS * EARTH_MEAN_RADIUS_KM;
    public static final double KM_TO_DEG = 1 / DEG_TO_KM;

    public static final double EARTH_MEAN_RADIUS_MI = EARTH_MEAN_RADIUS_KM * KM_TO_MILES;
    public static final double EARTH_EQUATORIAL_RADIUS_MI = EARTH_EQUATORIAL_RADIUS_KM * KM_TO_MILES;
}
