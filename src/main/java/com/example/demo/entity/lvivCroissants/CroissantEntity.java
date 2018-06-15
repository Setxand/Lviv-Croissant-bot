package com.example.demo.entity.lvivCroissants;

import com.example.demo.dto.uniRequestModel.CroissantDTO;
import com.example.demo.entity.peopleRegister.TUser;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
public  class CroissantEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer price;
    private String imageUrl;
    private Long creatorId;
    @OneToMany(mappedBy = "croissantEntity", cascade = CascadeType.ALL)
    private List<CroissantsFilling> croissantsFillings = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "tUser_id")
    private TUser tUser;


    public CroissantEntity() {
    }




    public CroissantEntity(String name, String type) {
        this.name = name;
        this.type = type;

    }



    public void setCroissantsFillings(List<CroissantsFilling> croissantsFillings) {
        for(CroissantsFilling croissantsFilling : croissantsFillings){
            croissantsFilling.setCroissantEntity(this);
        }
        this.croissantsFillings = croissantsFillings;
    }

    public void addSingleFilling(CroissantsFilling croissantsFilling){
        this.getCroissantsFillings().add(croissantsFilling);
        croissantsFilling.setCroissantEntity(this);
    }



    @Override
    public String toString() {
        return "\n\n"+name+".\n"+"" +
                "Вміст: \n" +
                croissantsFillings;
    }


}
