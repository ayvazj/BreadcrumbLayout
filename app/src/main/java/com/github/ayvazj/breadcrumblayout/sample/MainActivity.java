package com.github.ayvazj.breadcrumblayout.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.github.ayvazj.breadcrumblayout.BreadcrumbLayout;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private BreadcrumbLayout breadcrumbLayout;
    private Button addButton;
    private Button removeButton;
    private Random rand;
    private ImageView imageView;

    enum Pokemon {
        Bulbasaur("Bulbasaur", R.drawable.pokemon_1),
        Ivysaur("Ivysaur", R.drawable.pokemon_2),
        Venusaur("Venusaur", R.drawable.pokemon_3),
        Charmander("Charmander", R.drawable.pokemon_4),
        Charmeleon("Charmeleon", R.drawable.pokemon_5),
        Charizard("Charizard", R.drawable.pokemon_6),
        Squirtle("Squirtle", R.drawable.pokemon_7),
        Wartortle("Wartortle", R.drawable.pokemon_8),
        Blastoise("Blastoise", R.drawable.pokemon_9),
        Caterpie("Caterpie", R.drawable.pokemon_10),
        Metapod("Metapod", R.drawable.pokemon_11),
        Butterfree("Butterfree", R.drawable.pokemon_12),
        Weedle("Weedle", R.drawable.pokemon_13),
        Kakuna("Kakuna", R.drawable.pokemon_14),
        Beedrill("Beedrill", R.drawable.pokemon_15),
        Pidgey("Pidgey", R.drawable.pokemon_16),
        Pidgeotto("Pidgeotto", R.drawable.pokemon_17),
        Pidgeot("Pidgeot", R.drawable.pokemon_18),
        Rattata("Rattata", R.drawable.pokemon_19),
        Raticate("Raticate", R.drawable.pokemon_20),
        Spearow("Spearow", R.drawable.pokemon_21),
        Fearow("Fearow", R.drawable.pokemon_22),
        Ekans("Ekans", R.drawable.pokemon_23),
        Arbok("Arbok", R.drawable.pokemon_24),
        Pikachu("Pikachu", R.drawable.pokemon_25);

        private final String name;
        private final int drawableRes;

        Pokemon(String name, int drawableRes) {
            this.name = name;
            this.drawableRes = drawableRes;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        breadcrumbLayout = (BreadcrumbLayout) findViewById(R.id.breadcrumbLayout);
        imageView = (ImageView) findViewById(R.id.imageView);

        addButton = (Button) findViewById(R.id.addCrumb);
        removeButton = (Button) findViewById(R.id.removeCrumb);
        rand = new Random();


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pokemon randomPokemon = Pokemon.values()[rand.nextInt(Pokemon.values().length - 1)];
                breadcrumbLayout.addCrumb(breadcrumbLayout.newCrumb().setText(randomPokemon.name).setTag(randomPokemon));
                imageView.setImageResource(randomPokemon.drawableRes);
            }
        });

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                breadcrumbLayout.removeCrumbAt(breadcrumbLayout.getCrumbCount() - 1);
            }
        });

        breadcrumbLayout.addCrumb(breadcrumbLayout.newCrumb().setText("POKEMON"));

        breadcrumbLayout.setOnBreadcrumbSelectedListener(new BreadcrumbLayout.OnBreadcrumbSelectedListener() {
            @Override
            public void onBreadcrumbSelected(BreadcrumbLayout.Breadcrumb crumb) {
                if ( crumb.getTag() != null && crumb.getTag() instanceof Pokemon) {
                    imageView.setImageResource(((Pokemon)crumb.getTag()).drawableRes);
                }
                else {
                    imageView.setImageDrawable(null);
                }
            }

            @Override
            public void onBreadcrumbUnselected(BreadcrumbLayout.Breadcrumb crumb) {

            }

            @Override
            public void onBreadcrumbReselected(BreadcrumbLayout.Breadcrumb crumb) {

            }
        });
    }
}
