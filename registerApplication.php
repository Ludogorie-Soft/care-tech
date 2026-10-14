<?php

require_once 'Cryptor.php';

$data_array = [
    "orderid" => "0",
    "firstname" => "Тест",
    "lastname" => "Тестов",
    "surname" => "",
    "email" => "ngeorgiev@tbibank.bg",
    "phone" => "0879256031",
    "deliveryaddress" => [
        "country" => "Bulgaria",
        "county" => "",
        "city" => "София",
        "streetname" => "ГМ Димитров",
        "streetno" => "62",
        "buildingno" => "",
        "entranceno" => "",
        "floorno" => "5",
        "apartmentno" => "35",
        "postalcode" => ""
    ],
    "items" => [	
        [
            "name" => "Спортно елегантен панталон",
            "description" => "",
            "qty" => "3",
            "price" => "34.99",
            "sku" => "3345",
            "category" => "255",
            "imagelink" => "https://tbi-uat.online/hat/hat.jpg"
        ],
        [			
            "name" => "Блуза раета",
            "description" => "",
            "qty" => "1",
            "price" => "90.65",
            "sku" => "5566",
            "category" => "255",
            "imagelink" => "https://tbi-uat.online/hat/hat.jpg"
        ]
    ],
		"period" => 34,
    "successRedirectURL" => "https://success.com",
    "failRedirectURL" => "https://fail.com",
];

$json_data = json_encode($data_array);

$encryption_key = 'd1c2e12cfeababc8b95daf6902e210b170992e68fd1c1f19565a40cf0099c6e2cb559b85d7c14ea05b4dca0a790656d003ccade9286827cffdf8e664fd271499';
$encrypted_data = Cryptor::Encrypt($json_data,$encryption_key);


$curl = curl_init();

curl_setopt_array($curl, array(
  CURLOPT_URL => 'https://beta.tbibank.support/api/RegisterApplication',
  CURLOPT_RETURNTRANSFER => true,
  CURLOPT_ENCODING => '',
  CURLOPT_MAXREDIRS => 10,
  CURLOPT_TIMEOUT => 5,
  CURLOPT_FOLLOWLOCATION => true,
  CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
  CURLOPT_CUSTOMREQUEST => 'POST',
  CURLOPT_POSTFIELDS =>'{
    "reseller_code":"TBI1",
    "reseller_key":"EX1",
    "data":"'.$encrypted_data.'"
}
',
  CURLOPT_HTTPHEADER => array(
    'Content-Type: application/json'
  ),
));

$response = curl_exec($curl);

curl_close($curl);
echo $response;

?>