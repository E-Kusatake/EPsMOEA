# coding: UTF-8
import networkx as nx
import numpy as np
from numpy.random import *
import math
import matplotlib.pyplot as plt
from functools import wraps
import time
import csv
import pprint
import pandas as pd
import seaborn as sns
from statistics import mean, median, variance, stdev
import copy
#from platypus.problems import Problem
#from platypus.algorithms import NSGAII, NSGAIII, GDE3, CMAES, IBEA, MOEAD, EpsMOEA, SPEA2, OMOPSO, SMPSO
#from platypus.types import Constraint, Binary, Real
from platypus import NSGAII, NSGAIII, GDE3, CMAES, IBEA, MOEAD, EpsMOEA, SPEA2, OMOPSO, SMPSO, Problem, Constraint, Binary, Real
# 'NSGAIII' works for more than three optimization problems
# 'MOEAD' only works minimization problems
# 'CMAES' takes huge time to find solutions

np.set_printoptions(threshold=np.inf)
pd.set_option('display.max_columns', 100)

# ----------------------------
# constant parameters for simulation
# ----------------------------
NUM_SAMPLES = 10000 # number of sumples
population = 10 # default = 100

N = 10 # number of room
iteration = 1000 # iteration
jikan = 1 # trading period
p_s = 0.01855  # 18.55yen/kWh selling price of consumers
p_p = 0.00805   # 8.05yen/kWh purchasing price of public utilty
p_m = 0.02905  # 29.05yen/kWh selling price of public utilty

which_algorithm = 'EPsMOEA' # which algorithm is executed

# ----------------------------
# creating tuple denoting columns
# ----------------------------
def make_tuple():
    temp_tuple = tuple(["x"+str(i+1) for i in range(len(G.edges()))])
    constraints_tuple = tuple(["c"+str(j+1) for j in range(N)])
    consumers_tuple = tuple(["p"+str(j+1) for j in range(N)])
    return temp_tuple + constraints_tuple + ("f1", "f2") + consumers_tuple

# ----------------------------
# method for printing experimental result
# ----------------------------
def print_result(algorithm):
    # columns in df_result
    columns_tuple = make_tuple()
    df_result = pd.DataFrame(columns = columns_tuple)

    # add reults to DataFrame
    for i in range(len(algorithm.result)):
      df_result.loc[i] = algorithm.result[i].variables[:] + \
        algorithm.result[i].constraints[:] \
        + algorithm.result[i].objectives[:] + \
        [0] * N
        # back slash　→ make a new line

    # write results into a CSV file

    # plot the results on GUI
    # sns.pairplot(df)
    # plt.show()

    return df_result

# ----------------------------
# calculating standard deviation 使ってない
# ----------------------------
def calc_deviation(consumer_trading_volume): # parameter should be set to each consumer's benefit

    sum_eq = 0 # list index out of range

    for idx in range(N):
        sum_eq = sum_eq + pow((np.mean(consumer_trading_volume) - consumer_trading_volume[idx]), 2)

    standard_deviation = math.sqrt(sum_eq / N)

    return standard_deviation

# ----------------------------
# calculating each consumer's payoff list
# ----------------------------
def consumer_payoff(var_array):
    # create consumer's payoff_list
    consumer_trading_benefit = [0] * N
    for idx, edge in enumerate(G.edges()):
        if edge[0] == N or edge[1] == N: # trading with public utility
            pass
        else:
            for i in range(N):
                if i == edge[0]: # if i is seller
                    consumer_trading_benefit[i] += var_array[idx] * (p_s - p_p)
                elif i == edge[1]: # if i is buyer
                    consumer_trading_benefit[i] += var_array[idx] * (p_m - p_s)
                    # var_array -> trading volume in each edge
                    # consumer_trading_volume -> each prosumer's trading volume in P2P trading

    return consumer_trading_benefit

# ----------------------------
# setting standard deviation of normalized payoff
# ----------------------------

def set_min_normalized_deviation(var_array):
    payoff_list = [coef[i] * var_array[i] / demand[i] for i in range(len(G.edges()))]
    standard_deviation = calc_deviation(payoff_list)
    return standard_deviation

# ----------------------------
# setting standard deviation of obtained payoff
# ----------------------------
def set_min_deviation(var_array):
    payoff_list = [coef[i] * var_array[i] for i in range(len(G.edges()))]
    consumer_trading_benefit = consumer_payoff(var_array)
    standard_deviation = calc_deviation(consumer_trading_benefit)
    return standard_deviation

# ----------------------------
# setting total payoff
# ----------------------------
def set_max_payoff(var_array):
    objectives_elements = [coef[i] * var_array[i] for i in range(len(G.edges()))]
    # define the first objective function (maximization of the total of consumer's payoff)
    # objectives_elements = [(p_m-p_s) * var_array[i] for i in range(len(G.edges()))]
    total_payoff_equation = objectives_elements[0]
    for i in range(1, len(G.edges())):
        total_payoff_equation = total_payoff_equation + objectives_elements[i]

    return total_payoff_equation

# ----------------------------
# setting objective functions
# ----------------------------
def set_objective(var_array, constraint_array):
    objective_array = [] #array of objective functions

    objective_max_payoff = set_max_payoff(var_array)
    objective_array.append(objective_max_payoff)
    objective_min_deviation = set_min_deviation(var_array)

    # objective_min_deviation = set_min_normalized_deviation(var_array)

    objective_array.append(objective_min_deviation)

    return objective_array

# ----------------------------
# setting constraints of optimization problem
# ----------------------------
def set_constraint(var_array):
    constraint_array = []
    sum_decision_var = [0] * N

    for idx, edge in enumerate(G.edges()):
        if edge[0] == N:
            sum_decision_var[edge[1]] = sum_decision_var[edge[1]] + var_array[idx]
        elif edge[1] == N:
            sum_decision_var[edge[0]] = sum_decision_var[edge[0]] + var_array[idx]
        else:
            sum_decision_var[edge[0]] = sum_decision_var[edge[0]] + var_array[idx] # total supply or demand electricity
            sum_decision_var[edge[1]] = sum_decision_var[edge[1]] + var_array[idx]

    constraint_array = np.append(constraint_array, abs(deff[:-1]) - sum_decision_var)

    return constraint_array

# ----------------------------
# base definition of optimization promlem
# ----------------------------
def opt_problem(x):
    #array of decision variables
    var_array = [x[i] for i in range(len(G.edges()))]
    # このvar_arrayは各枝の取引量を表す．
    # 各プロシューマの取引量に直すと...
    consumer_trading_volume = consumer_payoff(var_array) # 各プロシューマの取引
    constraint_array = set_constraint(var_array)
    objective_array = set_objective(var_array, constraint_array)
    return objective_array, constraint_array

# ----------------------------
# calling a function to solve problems of prosumers
# ----------------------------
def calculate_result():
    problem = Problem(len(G.edges()), 2, N)
    # Problemの引数が3個 → 変数の数, 目的関数の数, 制約関数の数
    # Problemの引数が2個 → 変数の数, 目的関数の数

    # optimization problem is either maximization or minimization
    problem.directions[:] = [Problem.MAXIMIZE, Problem.MINIMIZE]

    # inequation of constraints
    problem.constraints[:] = "==0"

    # constraints on each decision variable

    trading_capacity = [0]*len(G.edges())

    for idx, edge in enumerate(G.edges()):
        trading_capacity[idx] = min(abs(deff[edge[0]]), abs(deff[edge[1]]))
    types_array = [Real(0, trading_capacity[i]) for i in range(len(G.edges()))]
    problem.types[:] = types_array

    # objective function
    problem.function = opt_problem
    #print(opt_problem)

    # instantiate the optimization algorithm
    algorithm = EpsMOEA(problem, epsilons = [0.05], population_size = population)


    # optimize the prooblem using 10,000 function evaluetions
    algorithm.run(NUM_SAMPLES)

    # display the results
    #for solution in algorithm.result:
    #    print(solution.objectives)
    return algorithm

# ----------------------------
# create DiGraph
# ----------------------------

def create_digraph():
    G = nx.DiGraph() # create directed graph

    #consumers_sellers_idx = []
    #consumers_buyers_idx = []
    G.clear()
    for num in range(N): # add nodes
        G.add_node(num+1) # # of consumres and public utility

    sign = list(map(lambda x: 0 if x == 0 else int(math.copysign(1, x)), consumers_deff)) # decision sign

    seller_idx = [i for i, x in enumerate(sign) if x == 1]
    buyer_idx = [j for j, y in enumerate(sign) if y == -1]

    seller_idx.append(N)
    buyer_idx.append(N)

    # create diected graph ※ not allow graph to have loop and multiple edges
    for s_idx in seller_idx:
        for b_idx in buyer_idx:
            if s_idx == N and b_idx == N:
                pass
            else:
                G.add_edge(s_idx, b_idx)

    # coefficient of optimization problem
    benefit_coef = [0] * len(G.edges())
    for idx, edge in enumerate(G.edges()):
        if edge[0] == N or edge[1] == N:
            benefit_coef[idx] = 0
        else:
            benefit_coef[idx] = (p_m - p_s) + (p_s - p_p) # benefit by unit
    return(G, benefit_coef)

# ----------------------------
# main -> not implement this program when other programs import
# ----------------------------

if __name__ == '__main__':
    start = time.time()
    total_percentage = 0
    last_count = 0
    each_consumer_benefit = [0] * N
    total_consumer_benefit = [0]
    standard_deviation = [0]
    consumer_maximum_benefit = np.ones((iteration, N))
    # total_consumer_maximum_benefit = [0] * N
    consumers_tuple = tuple(["p"+str(j+1) for j in range(N)])
    name_columns = tuple(["obtained benefit", "maximum benefit", "percentage"])
    df_final_parameter = tuple(["F1", "F2", "percentage", "last_percentage"])
    df_final = pd.DataFrame(index = consumers_tuple, columns = name_columns)
    df_function = pd.Series(index = df_final_parameter)

    columns_tuple_others = ("f1", "f2") + consumers_tuple

    df_f1_maximum = pd.DataFrame(columns = columns_tuple_others)
    df_f2_minimum = pd.DataFrame(columns = columns_tuple_others)
    df_f1max_f2min = pd.DataFrame(columns = columns_tuple_others)
    df_random = pd.DataFrame(columns = columns_tuple_others)

    c_capacity = [[0.0 for i in range(40)] for j in range(1000)]
    c_demand = [[0.0 for i in range(40)] for j in range(1000)]

    with open('C:\\Users\\Eiichi Kusatake\\python\\sample_data\\c_s3_b7.csv') as f, open('C:\\Users\\Eiichi Kusatake\\python\\sample_data\\d_s3_b7.csv') as g:
        reader_capacity = csv.reader(f)
        reader_demand = csv.reader(g)
        i = j = 0
        for row_c in reader_capacity:
            c_capacity[i] = row_c
            i += 1
        for row_d in reader_demand:
            c_demand[j] = row_d
            j += 1

    for i in range(len(c_capacity)):
        for j in range(len(c_capacity[i])):
            c_capacity[i][j] = float(c_capacity[i][j])

    for i in range(len(c_demand)):
        for j in range(len(c_demand[i])):
            c_demand[i][j] = float(c_demand[i][j])
    for itr in range(iteration):
        print('# --------------------------------------')
        print('# iteration' , itr)
        print('# --------------------------------------')
        # create DataFrame
        # edge tuple
        # edges_tuple = tuple(["e"+str(k+1) for k in range(len(G.edges()))])
        # generation tuple
        generation_tuple = tuple(["g"+str(i+1) for i in range(N)])
        # consumption tuple
        consumption_tuple = tuple(["d"+str(j+1) for j in range(N)])
        # rate tuple
        rate_tuple = tuple(["selling_pu", "selling_pro", "purchasing_pu"])
        # parameter tuple
        parameter_tuple = tuple(["edges"]) + generation_tuple + consumption_tuple + rate_tuple + tuple(["percentage"])
        # create DataFrame
        df_parameter = pd.DataFrame(columns = parameter_tuple)


        real_trading_volume = [0] * N

        for t in range(jikan):
            total_count = 0
            for con in range(N):
                real_trading_volume[con] = 0

            # print('# --------------------------------------')
            # print('# time' , t)
            # print('# --------------------------------------')

            # each consumer's benefit
            consumer_benefit = [0] * N

            # p_m = np.round(normal(15, 1), 2) # price offered by main grid (random price)

            # set capacity and demand (numpy ndarray)
            # consumers_capacity = normal(549, 1, N) # /Wh total of consumer's capacity
                                                   # kWhに設定するとマイナスになることがある
            # consumers_demand = normal(502, 1, N)   # /Wh consumer's demand
            #consumers_capacity = normal(500, 1, N) # /Wh total of consumer's capacity
                                           # kWhに設定するとマイナスになることがある
            #consumers_demand = normal(500, 1, N)
            # 下限, 上限, 要素数
            consumers_capacity = np.array(c_capacity[itr])
            consumers_demand = np.array(c_demand[itr])
            #consumers_capacity = [10, 0, 0]
            #consumers_demand = [0, 10, 10]

            # either excess or deficit electricity
            consumers_deff = (consumers_capacity - consumers_demand)
            consumers_abs = abs(consumers_capacity - consumers_demand)
            #consumers_deff = [10, -10, -10]
            #consumers_abs = [10, 10, 10]
            capacity = np.append(consumers_capacity, 10000000000000) # append public utility's capacity
            demand = np.append(consumers_demand, 1000000000000) # append public utility's demand

            G, coef = create_digraph() # DiGraph and edges of coefficient (1単位あたりの利得)

            for o in range(N):
                G.nodes[o]['diff'] = consumers_abs[o]
            if len(G.edges()) == 0: # if # of edges is equal to 0, participants do nothing in the market
                pass # print('# no trading --------------------------')
            else:
                # append defference btw consumer's excess and deficit electricity + pubilic utility's capacity & demand ∞
                deff = np.append(consumers_deff, 10000000000000)

                # solve a dual-objectives optimization problem
                algorithm = calculate_result()

                # print result as a CSV file
                df_result = print_result(algorithm)

                for r, w in enumerate(G.edges()):
                    G.edges[w[0], w[1]]['volume'] = 0

                # 100行
                for result_idx in range(len(algorithm.result)):
                    count = 0
                    # the # of edges, extract index and edges
                    for k in range(N):
                        consumer_benefit[k] = 0
                    for idx, edge in enumerate(G.edges()):
                        G.edges[edge[0], edge[1]]['volume'] += algorithm.result[result_idx].variables[idx]
                        if edge[0] == N or edge[1] == N: # if participants trade with public utility
                            pass
                            # prosumer's benefit is equal to 0
                        else:
                            #consumer_benefit[edge[0]] += df_result.loc[result_idx, "x"+str(idx+1)] * (p_s - p_p)
                            #consumer_benefit[edge[1]] += df_result.loc[result_idx, "x"+str(idx+1)] * (p_m - p_s)
                            consumer_benefit[edge[0]] += algorithm.result[result_idx].variables[idx] * (p_s - p_p)
                            consumer_benefit[edge[1]] += algorithm.result[result_idx].variables[idx] * (p_m - p_s)

                            # consumer_benefit[edge[1]] += df_result.loc[result_idx, "x"+str(idx+1)] * (p_m - p_s)

                    for num in range(N):
                        df_result.loc[result_idx, "p" + str(num+1)] = consumer_benefit[num]
                        for le, edf in enumerate(G.edges()):
                            real_trading_volume[num] += G.edges[edf[0], edf[1]]['volume']

                    for num in range(N):
                        if consumers_deff[num] > 0:
                            consumer_maximum_benefit[itr][num] = G.nodes[num]['diff'] * (p_s - p_p)
                        elif consumers_deff[num] < 0:
                            consumer_maximum_benefit[itr][num] = G.nodes[num]['diff'] * (p_m - p_s)
                        #print('real_trading_volume', real_trading_volume[num])
                        #print('node cap ', G.nodes[num]['diff'])
                        if real_trading_volume[num] > G.nodes[num]['diff']:
                            count += 1
                    # print('result_idx ', result_idx)
                    # print('# of algorithm ', len(algorithm.result) - 1)
                    # last solution は実行可能解か? percentage を出したい
                    if result_idx == len(algorithm.result) - 1:
                        if count > 0:
                            last_count += 1

                    if count > 0:
                        total_count += 1
                    #print('total_count ', total_count)

                df_result.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\Result\\Result_' + which_algorithm + '_SAMPLE'+str(NUM_SAMPLES)+'_exp_time' + str(t) + '_itr_' + str(itr) +'.csv')

                # rand_num = np.random.randint(len(algorithm.result))
                #df_final_result.loc[t] = df_result.loc[rand_num, :]
                # index of maximized F1
                idx_max = df_result['f1'].idxmax()
                # index of minimized F2f
                idx_min = df_result['f2'].idxmin()
                # index of maxmized F1 and minized F2
                max_list = [i for i, v in enumerate(df_result['f1']) if v == max(df_result['f1'])]
                temp_value = 10000000000000
                for j in max_list:
                    if temp_value > df_result['f2'].loc[j]:
                        idx_maxmin = j
                # of edges + # of constraints + # of functions
                index_benefit = len(G.edges()) + N + 2
                rand_num = np.random.randint(len(algorithm.result))

                df_f1_maximum.loc[itr] = algorithm.result[idx_max].objectives[:] + df_result.iloc[idx_max, index_benefit:].values.tolist()
                df_f2_minimum.loc[itr] = algorithm.result[idx_min].objectives[:] + df_result.iloc[idx_min, index_benefit:].values.tolist()
                df_f1max_f2min.loc[itr] = algorithm.result[idx_maxmin].objectives[:] + df_result.iloc[idx_maxmin, index_benefit:].values.tolist()
                df_random.loc[itr] = algorithm.result[rand_num].objectives[:] + df_result.iloc[rand_num, index_benefit:].values.tolist()
                # consumer_payoff_listを持ってきたい（）
                # consumer_payoff_list = consumer_payoff(var_array)
                # calculate each consumers' benefit

                # algorithm_length = len(algorithm.result[:])-1
                # initialized parameters

                # edges_element = [edge for idx, edge in enumerate(G.edges())]
                edges_element = [G.edges()]
                demand_element = [consumers_demand[m] for m in range(N)]
                generation_element = [consumers_capacity[l] for l in range(N)]
                rate_element = [p_m, p_s, p_p]

                # 実行可能解 %
                percentage = ((population - total_count) / population) * 100
                df_parameter.loc[t] = edges_element + generation_element + demand_element + rate_element + [percentage]
                # 全体の実行可能解 percentage を足していく
                total_percentage += percentage

                avg_df = df_result.mean()

                each_consumer_benefit += avg_df[-N:].values
                total_consumer_benefit += avg_df[-(N+2):-(N+1)].values
                standard_deviation += avg_df[-(N+1):-(N)].values

        df_parameter.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPsMOEA\\Parameter\\Parameter_' + which_algorithm + '_SAMPLE_' + str(NUM_SAMPLES) + '_itr_' + str(itr) + '.csv')
    df_f1_maximum.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\F1\\F1_MAX_platypus_' + which_algorithm + '_SAMPLE_' + str(NUM_SAMPLES) + '_itr_' + str(itr) + '.csv')
    df_f2_minimum.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\F2\\F2_MIN_platypus_' + which_algorithm + '_SMAPLE_' + str(NUM_SAMPLES) + '_itr_' + str(itr) + '.csv')
    df_f1max_f2min.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\F1_F2\\F1_MAX_F2_MIN_platypus_' + which_algorithm + '_SMAPLE_' + str(NUM_SAMPLES) + '_itr_' + str(itr) + '.csv')
    df_random.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\Random\\Random_' + which_algorithm + '_SAMPLE' + str(NUM_SAMPLES) + '_itr_' + str(itr) + '_representative.csv')

    # total_consumer_maximum_benefit = np.sum(consumer_maximum_benefit, axis=0)
    # 最後の世代の解の実行可能解の確率
    last_percentage = [((iteration * jikan - last_count) / iteration * jikan ) * 100]

    # 全ての解の実行可能解の確率
    avg_percentage = [total_percentage / (iteration * jikan)]

    # 各プロシューマの利得の平均
    array_1 = each_consumer_benefit / (iteration * jikan)
    avg_each_consumer_benefit = array_1.tolist()

    # プロシューマの利得の総和の平均
    array_2 = total_consumer_benefit / (iteration * jikan)
    avg_total_consumer_benefit = array_2.tolist()

    # プロシューマの利得の標準偏差の平均
    array_3 = standard_deviation / (iteration * jikan)
    avg_standard_deviation = array_3.tolist()

    # ("f1", "f2") + consumers_tuple + ("per", "last_per")
    # print('last_percentage ', last_percentage) avg_total_consumer_benefit + avg_standard_deviation +  + avg_percentage + last_percentage
    df_final['obtained benefit'] = each_consumer_benefit
    df_final['maximum benefit'] = np.sum(consumer_maximum_benefit, axis=0)
    df_final['percentage'] = df_final['obtained benefit'] / df_final['maximum benefit']

    df_function["F1"] = avg_total_consumer_benefit[0]
    # avg_total_consumer_benefit
    df_function["F2"] = avg_standard_deviation[0]
    df_function["percentage"] = avg_percentage[0]
    df_function["last_percentage"] = last_percentage[0]

    df_function.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\Function\\Function_platypus_' + which_algorithm + '_SAMPLE_' + str(NUM_SAMPLES) + '.csv')
    df_final.to_csv('C:\\Users\\Eiichi Kusatake\\python\\s3_b7\\EPSMOEA\\Final\\Final_platypus_' + which_algorithm + '_SAMPLE_' + str(NUM_SAMPLES) + '.csv')

#nx.draw_networkx(G)
#plt.show()

elapsed_time = time.time() - start
print(which_algorithm)
print('NUM_SAMPLES' + str(NUM_SAMPLES))
print('population' + str(population))
print('iteration' + str(iteration))
print("elapsed_time:{0}".format(elapsed_time) + "[sec]")
# reference
# https://qiita.com/TatsuyaKatayama/items/ccdc51d8d9570b30c538
# https://github.com/Project-Platypus/Platypus/blob/87e744e09618bda46ee80f0e117337b6e582c588/examples/knapsack.py
