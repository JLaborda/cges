#!/bin/bash

algorithm_names=("cges" "ges" "fes")
net_names=("andes" "link" "munin")
net_paths=("./res/networks/andes/andes.xbif" "./res/networks/link/link.xbif" "./res/networks/munin/munin.xbif")
dataset_paths=()
for net_name in "${net_names[@]}"
do
    for i in {0..10}
    do
      if (( i < 10 )); then
          dataset_paths+=("./res/datasets/${net_name}/${net_name}0${i}.csv")
      else
          dataset_paths+=("./res/datasets/${net_name}/${net_name}${i}.csv")
      fi
    done
done
number_cges_threads=("1" "2" "4" "8")

for ((i=0; i<${#net_names[@]}; i++))
do
    net_name="${net_names[i]}"
    net_path="${net_paths[i]}"

    # edge_limitation_calc = 10 /k*sqrt(n)
    if [[ $net_name == "link" ]]; then
        edge_limitation_calc=$(bc -l <<< "scale=0; 10/${number_cges_threads[i]} * sqrt(724)")
    elif [[ $net_name == "andes" ]]; then
        edge_limitation_calc=$(bc -l <<< "scale=0; 10/${number_cges_threads[i]} * sqrt(223)")
    elif [[ $net_name == "munin" ]]; then
        edge_limitation_calc=$(bc -l <<< "scale=0; 10/${number_cges_threads[i]} * sqrt(1041)")
    fi

    echo "algorithm_name net_name net_path dataset_path number_cges_threads edge_limitation" > "${net_name}_parameters.txt"

    for algorithm_name in "${algorithm_names[@]}"
    do
        for dataset_path in "${dataset_paths[@]}"
        do
            for number_cges_thread in "${number_cges_threads[@]}"
            do
                # Saving both the edge_limitation configuration and the non_edge_limitation configuration
                echo "$algorithm_name $net_name $net_path $dataset_path $number_cges_thread $edge_limitation_calc" >> "${net_name}_parameters.txt"
                echo "$algorithm_name $net_name $net_path $dataset_path $number_cges_thread 2147483647" >> "${net_name}_parameters.txt"
            done
        done
    done

    echo "Parameters file for ${net_name} generated successfully!"
done
