package yy.com.yjboretrofitcsdndemo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import yy.com.yjboretrofitcsdndemo.entity.NetWorkClass;
import yy.com.yjboretrofitcsdndemo.interf.HttpService;

/****
 * 基于Retrofit2，okhttp3的数据缓存（cache）技术
 * 2016年7月29日12:03:06
 *
 * @author yjbo
 * @qq:1457521527
 */
public class MainActivity extends AppCompatActivity {
    @Bind(R.id.show_result)
    TextView showResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initGet();
    }

    /***
     * 获取服务器数据
     */
    private void initGet() {
        //设置缓存
        File httpCacheDirectory = new File(MainActivity.this.getCacheDir(), "cache_responses_yjbo");
        Cache cache = null;
        try {
            cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);
        } catch (Exception e) {
            Log.e("OKHttp", "Could not create http cache", e);
        }
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(interceptor)
                .addNetworkInterceptor(interceptor)
                .addInterceptor(httpLoggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpService.baseHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        final HttpService service = retrofit.create(HttpService.class);
        Call<NetWorkClass> call = service.getFirstBlog();
        call.enqueue(new Callback<NetWorkClass>() {
            @Override
            public void onResponse(Call<NetWorkClass> call, retrofit2.Response<NetWorkClass> response) {
                Log.i("yjbo-获取数据：", "===");
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "数据请求成功", Toast.LENGTH_SHORT).show();
                    NetWorkClass netWorkClass = response.body();
                    showResult.setText(netWorkClass.toString());
                } else {
                    showResult.setText(response.code() + "--数据请求失败--");
                }
            }

            @Override
            public void onFailure(Call<NetWorkClass> call, Throwable t) {

            }
        });
    }

    /***
     * 拦截器，保存缓存的方法
     * 2016年7月29日11:22:47
     */
    Interceptor interceptor = new Interceptor() {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            if (checkNet(MainActivity.this)) {
                Response response = chain.proceed(request);
                int maxAge = 6; // 在线缓存在1分钟内可读取
                String cacheControl = request.cacheControl().toString();
                Log.e("yjbo-cache", "在线缓存在1分钟内可读取" + cacheControl);
                return response.newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                Log.e("yjbo-cache", "离线时缓存时间设置");
                request = request.newBuilder()
                        .cacheControl(FORCE_CACHE1)//此处设置了7秒---修改了系统方法
                        .build();

                Response response = chain.proceed(request);
                //下面注释的部分设置也没有效果，因为在上面已经设置了
                return response.newBuilder()
//                        .removeHeader("Pragma")
//                        .removeHeader("Cache-Control")
//                        .header("Cache-Control", "public, only-if-cached, max-stale=50")
                        .build();
            }
        }
    };
    //这是设置在多长时间范围内获取缓存里面
    public static final CacheControl FORCE_CACHE1 = new CacheControl.Builder()
            .onlyIfCached()
            .maxStale(7, TimeUnit.SECONDS)//这里是7s，CacheControl.FORCE_CACHE--是int型最大值
            .build();



    /***
     * 检查网络
     *
     * @param context
     * @return
     */
    public static boolean checkNet(Context context) {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                // 获取网络连接管理的对像
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info == null || !info.isAvailable()) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @OnClick(R.id.btn_again)
    public void onClick() {
        initGet();
    }
}
