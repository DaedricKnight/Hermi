package com.khateev.hermi.shared.ai

import com.khateev.hermi.shared.model.Command
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ONNXAIProcessorTest {

    @Test
    fun `test all 30 command variants mapping`() {
        val testCases = listOf(
            "check_air_filter" to Command.CheckAirFilter,
            "check_battery_health" to Command.CheckBatteryHealth,
            "check_battery_voltage" to Command.CheckBatteryVoltage,
            "check_brake_fluid" to Command.CheckBrakeFluid,
            "check_brake_pad_wear" to Command.CheckBrakePadWear,
            "check_coolant_level" to Command.CheckCoolantLevel,
            "check_driving_range" to Command.CheckDrivingRange,
            "check_engine" to Command.CheckEngineStatus,
            "check_ev_status" to Command.CheckEvStatus,
            "check_fuel_consumption" to Command.CheckFuelConsumption,
            "check_fuel_level" to Command.CheckFuelLevel,
            "check_odometer" to Command.CheckOdometer,
            "check_oil_level" to Command.CheckOilLevel,
            "check_outside_temperature" to Command.CheckOutsideTemperature,
            "check_rpm" to Command.CheckRpm,
            "check_spark_plug_condition" to Command.CheckSparkPlugCondition,
            "check_steering_status" to Command.CheckSteeringStatus,
            "check_suspension_health" to Command.CheckSuspensionHealth,
            "check_tire_wear" to Command.CheckTireWear,
            "check_tires" to Command.CheckTireStatus,
            "check_transmission_fluid" to Command.CheckTransmissionFluid,
            "check_transmission_status" to Command.CheckTransmissionStatus,
            "check_wheel_alignment" to Command.CheckWheelAlignment,
            "find_car_wash" to Command.FindCarWash,
            "find_charging_station" to Command.FindChargingStation,
            "find_food" to Command.FindFood,
            "find_gas_station" to Command.FindGasStation,
            "find_parking" to Command.FindParking,
            "find_rest_stop" to Command.FindRestStop,
            "find_service" to Command.FindService
        )

        assertEquals("Should have 30 test cases", 30, testCases.size)

        for ((label, expectedCommand) in testCases) {
            val result = CommandMapper.mapLabelToCommand(label, 0.9f, "some text")
            assertEquals("Mapping failed for label: $label", expectedCommand, result)
        }
    }

    @Test
    fun `test all commands phrases coverage`() {
        val allTestData = mutableMapOf<String, List<String>>()

        // ==================== FREE VERSION ====================
        allTestData["find_food"] = listOf(
            "I'm hungry", "Find a restaurant", "Nearest cafe", "I want pizza", "Looking for burgers",
            "Where can I eat?", "Best pasta nearby", "Coffee shop", "Breakfast spots", "Dinner options",
            "Fast food", "Healthy food", "Vegetarian places", "Vegan cafe", "Chinese food",
            "Italian cuisine", "Mexican food", "Indian restaurant", "Seafood places", "Bakery near me",
            "Ice cream", "Donut shop", "Sandwich shop", "Grill bar", "Pub with food",
            "Gastropub", "Food court", "Nearest bistro", "Steakhouse", "Take me to food"
        )
        allTestData["find_gas_station"] = listOf(
            "Low on gas", "Find gas station", "Nearest petrol station", "Where to refuel?", "Fuel station near me",
            "Petrol pump", "Gas prices", "Diesel station", "Cheap gas", "Shell station",
            "Exxon", "Chevron nearby", "BP station", "24h gas station", "Gas shop",
            "Fill up the tank", "Need petrol", "Gas on route", "Closest gas", "Fuel up",
            "Gas with car wash", "Truck stop gas", "High octane", "Unleaded gas", "Gas air pump",
            "Refueling station", "Gas now", "Where is fuel?", "Nearest pump", "Refill car"
        )
        allTestData["find_charging_station"] = listOf(
            "Battery low, find charger", "EV charging station", "Where to charge?", "Tesla supercharger", "Electric charging",
            "Nearest charging point", "Level 2 charger", "DC fast charger", "Charge point nearby", "Plug in station",
            "EV location", "Public charger", "Free EV charging", "Charging hub", "Need to charge",
            "EV station on route", "Electric fuel", "Charging dock", "Fast charging", "EV network",
            "Charge my electric car", "Charging spot", "Electric facilities", "EV map", "Where is charger?",
            "Power up EV", "Car battery station", "Energy station", "Rapid charger", "EV plug"
        )
        allTestData["find_parking"] = listOf(
            "Where to park?", "Find parking lot", "Parking garage", "Street parking", "Free parking",
            "Paid parking", "Safe parking", "Covered parking", "Underground parking", "Public parking",
            "Parking area", "Where to leave car?", "Parking space", "Multi-storey park", "Secure parking",
            "Mall parking", "Parking near park", "Accessible parking", "Long term parking", "Short term parking",
            "24/7 parking", "Parking with cameras", "Valet parking", "Park my car", "Find a spot",
            "Available parking", "Car park location", "Parking plaza", "Parking zone", "Where is the lot?"
        )
        allTestData["find_rest_stop"] = listOf(
            "I need a break", "Find rest area", "Rest stop nearby", "Highway rest area", "Roadside stop",
            "Where to rest?", "Toilet break", "Service area", "Picnic spot", "Driver rest area",
            "Scenic overlook", "Truck rest stop", "Night rest area", "Safe place to stop", "Resting place",
            "Stop for stretch", "Refreshment stop", "Quiet spot", "Highway service", "Travel plaza",
            "Restroom stop", "Public rest area", "Waypoint for rest", "Stop area", "Break station",
            "Pause the drive", "Where is rest stop?", "Road trip break", "Comfort station", "Visitor center"
        )

        // ==================== PRO VERSION ====================
        
        // ENGINE & POWERTRAIN
        allTestData["check_engine"] = listOf(
            "Is the engine light on?", "Check engine status", "Scan for error codes", "Are there any DTCs?", "Engine health check",
            "Any motor issues?", "Check powertrain", "Scan engine", "Troubleshoot engine", "Is my car okay?",
            "Monitor engine", "System diagnostic", "Read engine faults", "Check for engine warnings", "Is everything fine with the motor?",
            "Engine checkup", "Scan for malfunctions", "Check car health", "Verify engine condition", "Diagnostic report",
            "Is the check engine light active?", "Any problems with the drive unit?", "Check engine health", "Scan vehicle diagnostics", "Motor status",
            "Read trouble codes", "Identify engine errors", "Check for active faults", "Vehicle health scan", "Is the engine running normally?"
        )
        allTestData["check_oil_level"] = listOf(
            "Check oil level", "How is my oil?", "Is oil low?", "Monitor oil level", "Verify oil quantity",
            "Do I need more oil?", "Check lubricant level", "Oil check", "Is the oil sufficient?", "Check motor oil",
            "Is oil empty?", "Oil status", "Is the dipstick reading low?", "Check oil status", "Measure oil level",
            "Is oil pressure okay?", "Check for oil", "Current oil level", "Tell me the oil level", "Am I out of oil?",
            "Check my engine oil", "Is it time for oil?", "Oil level check", "Monitor lubricant", "Is my oil fine?",
            "Show me oil info", "Is the oil light on?", "Check oil amount", "Check fluid level oil", "How's the oil looking?"
        )
        allTestData["check_transmission_status"] = listOf(
            "Check transmission", "Gearbox status", "Is the transmission okay?", "Monitor gearbox", "Check for transmission issues",
            "Transmission health", "Is the transmission slipping?", "Check gear shifts", "Gearbox health", "Scan transmission",
            "Verify transmission condition", "Is the transmission overheating?", "Transmission check", "Check auto gearbox", "Transmission diagnostic",
            "Check gears", "Transmission monitor", "Is the transmission fluid hot?", "Gearbox checkup", "Transmission system status",
            "Check drivetrain", "Transmission error check", "Is my transmission fine?", "Check for gearbox codes", "Transmission performance",
            "Monitor drivetrain", "Check transmission health", "Transmission status report", "Is the trans okay?", "Check shift quality"
        )
        allTestData["check_brake_pad_wear"] = listOf(
            "Check brake pads", "Are my brakes worn?", "Brake pad health", "Monitor brake wear", "How thick are the pads?",
            "Brake pad thickness", "Check brake lining", "Verify brake pads", "Are my pads thin?", "Brake wear status",
            "Check front brakes", "Check rear brakes", "Is it time to change pads?", "Brake pad check", "Are the pads squeaking?",
            "Brake friction material", "Check brake condition", "Monitor pad wear", "Brake pad sensor check", "Are my brakes okay?",
            "Check brake health", "Pad wear diagnostic", "Brake system status", "Are the brakes thin?", "Check brake lining wear",
            "Brake pad status", "Monitor braking system", "Verify pad life", "Brake pad remaining life", "How are my brake pads?"
        )
        allTestData["check_spark_plug_condition"] = listOf(
            "Check spark plugs", "Spark plug health", "Are my plugs dirty?", "Monitor spark plugs", "Check ignition plugs",
            "Spark plug condition", "Verify spark plugs", "Are the plugs fouled?", "Check for misfires", "Spark plug check",
            "Is it time for new plugs?", "Check ignition system", "Spark plug life", "Check engine plugs", "Are the spark plugs okay?",
            "Spark plug status", "Monitor ignition", "Check plug gap", "Verify ignition plugs", "Spark plug diagnostic",
            "Check for spark issues", "Are the plugs worn?", "Spark plug checkup", "Check motor plugs", "Monitor cylinder plugs",
            "Spark plug performance", "Is the ignition system fine?", "Check for fouled plugs", "Check plug condition", "How are the spark plugs?"
        )
        allTestData["check_rpm"] = listOf(
            "Show current RPM", "Check engine speed", "What is the RPM?", "Monitor RPM", "Revolutions per minute",
            "Show tachometer", "Engine RPM status", "Is the RPM high?", "Current engine speed", "Check tach",
            "RPM monitor", "Show me RPM", "How fast is the motor spinning?", "Monitor engine revs", "RPM check",
            "Check engine revolutions", "Tachometer status", "Show revs", "What's the engine speed?", "Check idle RPM",
            "RPM diagnostic", "Monitor tachometer", "Show engine RPM", "Current RPM reading", "Check motor speed",
            "Is the RPM stable?", "Show tach reading", "Check engine rev count", "What are the revs?", "RPM info"
        )

        // FUEL & BATTERY
        allTestData["check_fuel_level"] = listOf(
            "How much fuel is left?", "Check fuel level", "Is the tank full?", "Monitor fuel", "Gas level check",
            "Am I low on fuel?", "Check gas level", "Fuel status", "Tell me the fuel level", "Is the fuel low?",
            "Show fuel info", "Current fuel level", "How much gas is in the car?", "Check petrol level", "Is the tank empty?",
            "Monitor gas", "Fuel quantity", "How's the gas level?", "Check tank level", "Am I out of gas?",
            "Fuel level status", "Show fuel gauge", "What's my fuel status?", "Is the gas light on?", "Check current fuel",
            "Monitor petrol level", "Gas level reading", "How much fuel is remaining?", "Check gas amount", "Fuel info"
        )
        allTestData["check_fuel_consumption"] = listOf(
            "Average fuel consumption", "Check gas mileage", "How's my MPG?", "Monitor fuel economy", "Show fuel consumption",
            "Fuel efficiency status", "Current MPG", "Check petrol usage", "How much fuel am I using?", "Fuel consumption monitor",
            "Check average fuel", "Monitor gas usage", "What's my fuel economy?", "Fuel usage status", "Check MPG",
            "How efficient is the car?", "Fuel consumption diagnostic", "Show gas mileage", "Current fuel economy", "Check fuel efficiency",
            "What is my MPG?", "Monitor fuel usage", "Show me fuel stats", "How is my fuel economy?", "Fuel economy reading",
            "Check gas usage", "Average consumption", "Show mileage info", "Current fuel usage", "How much gas am I burning?"
        )
        allTestData["check_battery_voltage"] = listOf(
            "Check battery voltage", "What is the voltage?", "Monitor battery volts", "Battery voltage status", "Current voltage",
            "Check volts", "Battery charge level", "Is the voltage low?", "Tell me battery voltage", "Battery volts check",
            "Monitor volts", "Show voltage info", "What's the voltage reading?", "Check battery charge", "Battery system voltage",
            "Is the battery charging?", "Voltage monitor", "Show battery volts", "What is the battery voltage?", "Check voltage level",
            "Battery voltage diagnostic", "Monitor battery charge", "Show me the volts", "How is the battery voltage?", "Voltage reading",
            "Check car battery voltage", "Average voltage", "Show electrical voltage", "Current volts", "How many volts?"
        )
        allTestData["check_battery_health"] = listOf(
            "Check battery health", "Is the battery okay?", "Battery status", "Monitor battery condition", "Battery health check",
            "Verify battery health", "Is the battery dying?", "Check for battery issues", "Battery life status", "Is the battery healthy?",
            "Battery condition check", "Battery health diagnostic", "Monitor battery life", "Check car battery", "Is my battery fine?",
            "Battery health report", "Verify battery condition", "Check battery system", "Battery performance", "Battery health monitor",
            "Is the battery good?", "Battery status report", "Monitor car battery", "Battery diagnostic", "Check for battery codes",
            "Is the battery aging?", "Battery health status", "Check battery life", "Is the battery charging properly?", "How is my battery?"
        )
        allTestData["check_ev_status"] = listOf(
            "Check EV status", "EV battery level", "Electric range", "Monitor EV battery", "Is the EV charging?",
            "EV status check", "Electric vehicle status", "Check charge level", "EV battery health", "Is the charger connected?",
            "Estimated EV range", "EV battery status", "Electric car info", "Monitor electric status", "EV charge time",
            "Check EV range", "How much charge is left?", "EV battery info", "Electric vehicle diagnostic", "Check EV charge",
            "Monitor electric vehicle", "Is the EV battery okay?", "EV system status", "Check electric battery", "How is the EV charge?",
            "EV charging info", "Electric range status", "Check EV battery life", "Is the EV connected?", "Electric status report"
        )

        // TIRES & SUSPENSION
        allTestData["check_tires"] = listOf(
            "Check tire pressure", "Are my tires okay?", "Tire status", "Monitor tire pressure", "Is a tire low?",
            "Tire pressure check", "Verify tires", "Are the tires flat?", "Tire pressure status", "Check for low tire",
            "Tire health", "Monitor tires", "Is the tire light on?", "Tire status check", "Check tire inflation",
            "Verify tire pressure", "Are the tires properly inflated?", "Tire pressure monitor", "Check air in tires", "Is the tire pressure low?",
            "Tire pressure diagnostic", "Monitor tire health", "Show tire info", "Are my tires fine?", "Tire pressure reading",
            "Check car tires", "Are the tires low?", "Show me tire stats", "Current tire pressure", "How are my tires?"
        )
        allTestData["check_tire_wear"] = listOf(
            "Check tire wear", "Are my tires bald?", "Tire tread depth", "Monitor tire wear", "How is the tire tread?",
            "Tire wear status", "Check for tire baldness", "Verify tire wear", "Are the tires worn out?", "Tire wear check",
            "Check tread wear", "Monitor tire tread", "Are my tires safe?", "Tire wear diagnostic", "Verify tread depth",
            "Check for uneven wear", "Are the tires thinning?", "Tire wear monitor", "Check tire condition", "Is the tread low?",
            "Tire wear status report", "Monitor tire condition", "Show tire wear info", "Are my tires old?", "Tire tread check",
            "Check car tire wear", "Are the tires balding?", "Show me tread stats", "Current tire wear", "How's the tire tread looking?"
        )
        allTestData["check_suspension_health"] = listOf(
            "Check suspension", "Suspension health", "Is the suspension okay?", "Monitor suspension", "Check for suspension issues",
            "Suspension status", "Is the ride bumpy?", "Check shocks and struts", "Suspension health check", "Verify suspension",
            "Is the suspension noisy?", "Suspension checkup", "Check for suspension noise", "Monitor ride quality", "Suspension system status",
            "Check struts", "Check shocks", "Is the car bouncing?", "Suspension diagnostic", "Check for leaks in suspension",
            "Are my shocks okay?", "Monitor suspension performance", "Suspension health report", "Verify suspension condition", "Check for suspension wear",
            "Is my suspension fine?", "Suspension status report", "Monitor chassis", "Check for spring issues", "Suspension info"
        )
        allTestData["check_wheel_alignment"] = listOf(
            "Check alignment", "Is the car pulling?", "Wheel alignment status", "Monitor alignment", "Check wheel alignment",
            "Is the steering centered?", "Verify alignment", "Check for alignment issues", "Alignment health", "Is the alignment off?",
            "Wheel alignment check", "Check if car pulls to side", "Verify wheel alignment", "Monitor wheel alignment", "Is the steering crooked?",
            "Check alignment diagnostic", "Check tracking", "Is the tracking okay?", "Alignment status report", "Monitor car alignment",
            "Check for uneven steering", "Verify car alignment", "Alignment health check", "Is the steering pulling?", "Check wheel tracking",
            "Alignment status", "Monitor steering alignment", "Check for alignment wear", "Is my alignment fine?", "Alignment info"
        )
        allTestData["check_steering_status"] = listOf(
            "Check steering", "Steering status", "Is the steering okay?", "Monitor steering", "Check for steering issues",
            "Steering health", "Is the steering heavy?", "Verify steering", "Check power steering", "Steering system status",
            "Is the steering loose?", "Steering checkup", "Check for steering noise", "Monitor steering performance", "Steering diagnostic",
            "Check for steering leaks", "Are my steering components okay?", "Monitor steering health", "Steering health report", "Verify steering condition",
            "Check for steering wear", "Is my steering fine?", "Steering status report", "Monitor steering system", "Check for rack and pinion issues",
            "Steering wheel status", "Is the steering stiff?", "Check for power steering fluid", "Is the steering vibration normal?", "Steering info"
        )

        // FLUIDS & FILTERS
        allTestData["check_transmission_fluid"] = listOf(
            "Check transmission fluid", "Is transmission fluid low?", "Transmission fluid status", "Monitor trans fluid", "Verify transmission fluid",
            "Is the trans fluid dirty?", "Check gearbox fluid", "Transmission fluid health", "Monitor gearbox oil", "Is transmission oil okay?",
            "Check transmission oil", "Verify gearbox fluid", "Is the transmission fluid pink?", "Transmission fluid check", "Check for trans fluid leaks",
            "Monitor transmission oil level", "Is the transmission fluid burnt?", "Transmission fluid diagnostic", "Check for gearbox fluid issues", "Verify transmission oil",
            "Is my transmission fluid fine?", "Transmission fluid status report", "Monitor gearbox fluid", "Check for transmission fluid quality", "Is the trans oil level okay?",
            "Transmission oil checkup", "Check for transmission fluid smell", "Is the transmission fluid clear?", "Transmission fluid info", "How's the transmission fluid?"
        )
        allTestData["check_brake_fluid"] = listOf(
            "Check brake fluid", "Is brake fluid low?", "Brake fluid status", "Monitor brake fluid", "Verify brake fluid",
            "Is the brake fluid dirty?", "Check for brake fluid leaks", "Brake fluid health", "Monitor brake system fluid", "Is brake oil okay?",
            "Check brake oil", "Verify brake system fluid", "Is the brake fluid clear?", "Brake fluid check", "Check for brake fluid issues",
            "Monitor brake fluid level", "Is the brake fluid contaminated?", "Brake fluid diagnostic", "Check for brake fluid quality", "Verify brake oil",
            "Is my brake fluid fine?", "Brake fluid status report", "Monitor brake fluid quality", "Check for brake fluid air", "Is the brake oil level okay?",
            "Brake oil checkup", "Check for brake fluid moisture", "Is the brake fluid level sufficient?", "Brake fluid info", "How's the brake fluid?"
        )
        allTestData["check_coolant_level"] = listOf(
            "Check coolant level", "Is coolant low?", "Antifreeze status", "Monitor coolant", "Verify coolant level",
            "Is the engine coolant okay?", "Check for coolant leaks", "Coolant health", "Monitor antifreeze", "Is the radiator fluid low?",
            "Check radiator fluid", "Verify antifreeze level", "Is the coolant level sufficient?", "Coolant check", "Check for antifreeze leaks",
            "Monitor coolant level", "Is the coolant dirty?", "Coolant diagnostic", "Check for coolant quality", "Verify engine coolant",
            "Is my coolant fine?", "Coolant status report", "Monitor radiator fluid", "Check for coolant smell", "Is the antifreeze level okay?",
            "Coolant checkup", "Check for radiator fluid quality", "Is the coolant level low?", "Coolant info", "How's the coolant?"
        )
        allTestData["check_air_filter"] = listOf(
            "Check air filter", "Is the air filter dirty?", "Air filter status", "Monitor air filter", "Verify air filter",
            "Is the engine filter okay?", "Check cabin filter", "Air filter health", "Monitor engine air filter", "Is the air filter clogged?",
            "Check for dirty filter", "Verify engine air filter", "Is the air filter clean?", "Air filter check", "Check for cabin filter issues",
            "Monitor air filter condition", "Is the air filter restricted?", "Air filter diagnostic", "Check for air filter wear", "Verify cabin filter",
            "Is my air filter fine?", "Air filter status report", "Monitor air filtration", "Check for air filter smell", "Is the engine filter clean?",
            "Air filter checkup", "Check for air filter dust", "Is the air filter okay?", "Air filter info", "How's the air filter?"
        )

        // STATS, ENV & FIND PRO
        allTestData["check_odometer"] = listOf(
            "Check odometer", "What is my mileage?", "Monitor odometer", "Odometer status", "Current mileage",
            "How many miles on the car?", "Show odometer reading", "Odometer check", "What's the total distance?", "Check mileage",
            "Monitor total miles", "Verify odometer", "How many kilometers?", "Odometer reading status", "Current car mileage",
            "What is the total mileage?", "Odometer diagnostic", "Show me the mileage", "What's the reading on the dash?", "Check car mileage",
            "Monitor distance traveled", "Verify total mileage", "How many miles have I driven?", "Odometer status report", "Current odometer reading",
            "What's the mileage now?", "Check total distance", "Show total miles", "What is the current mileage?", "Odometer info"
        )
        allTestData["check_driving_range"] = listOf(
            "Check driving range", "How far can I go?", "Monitor range", "Driving range status", "Current range",
            "What's my remaining distance?", "Show driving range", "Range check", "How many miles until empty?", "Check range",
            "Monitor distance until empty", "Verify range", "How many kilometers left?", "Driving range status report", "Current car range",
            "What is the total range?", "Range diagnostic", "Show me the range", "What's the distance to empty?", "Check car range",
            "Monitor remaining distance", "Verify total range", "How much further can I drive?", "Range status report", "Current driving range",
            "What's the range now?", "Check distance to empty", "Show remaining miles", "What is the current range?", "Driving range info"
        )
        allTestData["check_outside_temperature"] = listOf(
            "Check temperature", "What's the temp outside?", "Monitor outside temperature", "Ambient temperature status", "Current outside temp",
            "How cold is it?", "How hot is it?", "Show outside temperature", "Temperature check", "What is the ambient temp?",
            "Check outside air temp", "Monitor external temperature", "Verify outside temp", "What's the thermometer saying?", "Outside temperature status report",
            "Current air temperature", "What is the temperature outdoors?", "Temperature diagnostic", "Show me the temp", "What's the weather like?",
            "Check exterior temperature", "Monitor weather temperature", "Verify external air temp", "How's the temperature outside?", "Temperature status report",
            "Current outside air temp", "What's the temp now?", "Check outdoor temperature", "Show ambient temp", "Temperature info"
        )
        allTestData["find_service"] = listOf(
            "Find a mechanic", "Where is a service center?", "Auto repair shop nearby", "Find car service", "Locate a garage",
            "Where can I get my car fixed?", "Nearest mechanic", "Car repair nearby", "Service station location", "Find a repair shop",
            "Where is the dealership?", "Auto service near me", "Locate a mechanic", "Where to get an oil change?", "Nearest auto repair",
            "Find a garage", "Where's a good mechanic?", "Service center nearby", "Locate a car repair shop", "Where can I get a tune-up?",
            "Auto shop location", "Find a service station", "Where is a reliable mechanic?", "Nearest service center", "Locate a dealership",
            "Where can I get a diagnostic?", "Auto repair location", "Find a local mechanic", "Where is the nearest garage?", "Service info"
        )
        allTestData["find_car_wash"] = listOf(
            "Find a car wash", "Where is the nearest car wash?", "Car detailing nearby", "Find an auto wash", "Locate a car wash",
            "Where can I get my car washed?", "Nearest car wash", "Car cleaning nearby", "Car wash location", "Find a wash station",
            "Where is a touchless car wash?", "Auto wash near me", "Locate a car detailing shop", "Where to get a car wash?", "Nearest auto wash",
            "Find a car detailing service", "Where's a good car wash?", "Car wash nearby", "Locate a car cleaning service", "Where can I get a wax?",
            "Car wash shop location", "Find an automatic car wash", "Where is a self-service car wash?", "Nearest car cleaning", "Locate a wash center",
            "Where can I get an interior clean?", "Car wash location", "Find a local car wash", "Where is the nearest auto wash?", "Car wash info"
        )

        assertEquals("Should have 30 commands total", 30, allTestData.size)

        allTestData.forEach { (label, phrases) ->
            assertEquals("Command $label should have 30 phrases", 30, phrases.size)
            phrases.forEach { phrase ->
                val result = CommandMapper.mapLabelToCommand(label, 0.95f, phrase)
                assertTrue("Phrase '$phrase' with label '$label' should NOT be Unknown", result !is Command.Unknown)
            }
        }
    }

    @Test
    fun `mapLabelToCommand returns Unknown for low confidence`() {
        val result = CommandMapper.mapLabelToCommand("check_engine", 0.6f, "check engine")
        assertTrue(result is Command.Unknown)
    }

    @Test
    fun `softmax output sums to 1`() {
        val logits = floatArrayOf(1.0f, 2.0f, 3.0f)
        val result = CommandMapper.softmax(logits)
        assertEquals(1.0f, result.sum(), 0.0001f)
    }
}
