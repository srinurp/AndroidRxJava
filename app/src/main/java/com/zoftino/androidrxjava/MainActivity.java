package com.zoftino.androidrxjava;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private Disposable subscription;
    private SingleObserver observer;

    private Disposable multiThreadSubscription;
    private Observer multiThreadObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // observer gets result from thread background task and updates Ui on main thread
        //SingleObserver to receive single item from observable which emits single item
        observer = new SingleObserver<Float>() {
            @Override
            public void onSubscribe(Disposable d) {
                subscription = d;
            }
            @Override
            public void onSuccess(@NonNull Float aFloat) {
                TextView tv = (TextView) findViewById(R.id.result_one);
                tv.setText("" + aFloat);
            }
            @Override
            public void onError(Throwable e) {
                TextView tv = (TextView) findViewById(R.id.result_one);
                tv.setText("Error processing calculation");
            }
        };

        multiThreadObserver = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                multiThreadSubscription = d;
            }

            @Override
            public void onNext(@NonNull Integer val) {
                Log.d("updating UI onNext ", ""+Thread.currentThread().getName());
                TextView tv = (TextView) findViewById(R.id.result_one);
                tv.setText(tv.getText()+" "+ val);
            }
            @Override
            public void onError(Throwable e) {
                TextView tv = (TextView) findViewById(R.id.result_one);
                tv.setText("Error processing calculation");
            }

            @Override
            public void onComplete() {
                Log.d("updating UI onComplete ", ""+Thread.currentThread().getName());
                TextView tv = (TextView) findViewById(R.id.result_one);
                tv.setText(tv.getText()+" calculation complete");
            }
        };
    }

    public void divideNumber(View view) {
        //get two number from UI
        float numOne = Float.parseFloat(((EditText) findViewById(R.id.number_one)).getText().toString());
        float numTwo = Float.parseFloat(((EditText) findViewById(R.id.number_two)).getText().toString());

        //creates an observable using just for emitting input numbers
        //subscribeOn computation thread - to make it run in the background
        //observeOn main thread - to run the observer code in the main thread so UI can be updated
        //then creates list of floats using toList
        //apply function using map operator
        //subscribe to the observable emitted by map
        Observable.just(numOne, numTwo)
                .subscribeOn(Schedulers.computation())
                .toList()
                .map(new Function<List<Float>, Float>() {
                    @Override
                    public Float apply(List<Float> fl) throws Exception {
                        return fl.get(0) / fl.get(1);

                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);

    }

    public void rangeNumber(View view) {
        //get two number from UI
        int numOne = Integer.parseInt(((EditText) findViewById(R.id.number_one)).getText().toString());
        int numTwo = Integer.parseInt(((EditText) findViewById(R.id.number_two)).getText().toString());

        //creates observable which emits integers between give two numbers using range operator
        //subscribeOn computation scheduler leave android main thread
        //Each integer is converted into observable by flatMap and process parallel by subscribing it on compuation scheduler
        //Map applies function to calculate results
        //Observe on android main thread to update results in UI
        Observable.range(numOne, numTwo)
                .subscribeOn(Schedulers.computation())
                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer num) throws Exception {
                        Log.d("flatMap function ", ""+Thread.currentThread().getName());
                        //subscribe each observable on separate thread - parallel processing
                        return Observable.just(num).subscribeOn(Schedulers.computation());
                    }
                }).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer val) throws Exception {
                        Log.d("map calculation ", ""+Thread.currentThread().getName());
                        return val * (val + 1);
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(multiThreadObserver);
    }
    @Override
    protected void onDestroy() {
        //dispose subscription in case of active subscriptions to prevent memory leaks
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
        if (multiThreadSubscription != null && !multiThreadSubscription.isDisposed()) {
            multiThreadSubscription.dispose();
        }
        super.onDestroy();
    }
}
